package fastaimetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Ultra-fast, lock-free metrics collector and real-time evaluator for AI workflows.
 */
public class FastAIMetrics implements AutoCloseable {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");

    private final Path outputDirectory;
    private final int bufferFlushThreshold;
    private final List<AIMetricRecord> memoryBuffer = Collections.synchronizedList(new ArrayList<>(1024));
    private final List<MetricListener> listeners = new CopyOnWriteArrayList<>();

    // Lock-free global aggregate counters
    private final LongAdder totalPromptTokens = new LongAdder();
    private final LongAdder totalCompletionTokens = new LongAdder();
    private final LongAdder totalExecutionTimeNanos = new LongAdder();
    private final LongAdder totalCalls = new LongAdder();
    private final DoubleAdder totalCostUsd = new DoubleAdder();

    public FastAIMetrics() {
        this(null, 5000);
    }

    public FastAIMetrics(Path outputDirectory) {
        this(outputDirectory, 5000);
    }

    public FastAIMetrics(Path outputDirectory, int bufferFlushThreshold) {
        this.outputDirectory = outputDirectory;
        this.bufferFlushThreshold = bufferFlushThreshold;
        try {
            if (outputDirectory != null) {
                Files.createDirectories(outputDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize metrics log directory", e);
        }
    }

    /**
     * Records an AI model execution and calculates estimated cost automatically.
     */
    public AIMetricRecord record(
            String traceId,
            String model,
            int promptTokens,
            int completionTokens,
            long ttftNanos,
            long totalDurationNanos,
            float evaluationScore
    ) {
        double cost = AICostCalculator.calculateCost(model, promptTokens, completionTokens);
        AIMetricRecord record = new AIMetricRecord(
                traceId,
                model,
                promptTokens,
                completionTokens,
                ttftNanos,
                totalDurationNanos,
                cost,
                evaluationScore,
                System.currentTimeMillis()
        );

        // Update aggregates
        totalPromptTokens.add(promptTokens);
        totalCompletionTokens.add(completionTokens);
        totalExecutionTimeNanos.add(totalDurationNanos);
        totalCalls.increment();
        totalCostUsd.add(cost);

        memoryBuffer.add(record);

        for (MetricListener listener : listeners) {
            listener.onMetric(record);
        }

        if (outputDirectory != null && memoryBuffer.size() >= bufferFlushThreshold) {
            flush();
        }

        return record;
    }

    public void addListener(MetricListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MetricListener listener) {
        listeners.remove(listener);
    }

    public long getTotalPromptTokens() {
        return totalPromptTokens.sum();
    }

    public long getTotalCompletionTokens() {
        return totalCompletionTokens.sum();
    }

    public long getTotalTokens() {
        return totalPromptTokens.sum() + totalCompletionTokens.sum();
    }

    public long getTotalCalls() {
        return totalCalls.sum();
    }

    public double getTotalCostUsd() {
        return totalCostUsd.sum();
    }

    public double getAverageLatencyMillis() {
        long calls = totalCalls.sum();
        if (calls == 0) return 0.0;
        return (totalExecutionTimeNanos.sum() / (double) calls) / 1_000_000.0;
    }

    /**
     * Flushes currently buffered metrics to a timestamped .aimetrics file.
     */
    public synchronized void flush() {
        if (memoryBuffer.isEmpty() || outputDirectory == null) {
            return;
        }

        List<AIMetricRecord> snapshot;
        synchronized (memoryBuffer) {
            snapshot = new ArrayList<>(memoryBuffer);
            memoryBuffer.clear();
        }

        String fileName = LocalDateTime.now().format(FILE_DATE_FORMAT) + ".aimetrics";
        Path targetFile = outputDirectory.resolve(fileName);
        try {
            AIMetricsCodec.writeToFile(targetFile, snapshot);
        } catch (IOException e) {
            System.err.println("Failed to write metrics records to " + targetFile + ": " + e.getMessage());
        }
    }

    public List<AIMetricRecord> getBufferedRecords() {
        synchronized (memoryBuffer) {
            return new ArrayList<>(memoryBuffer);
        }
    }

    @Override
    public void close() {
        flush();
    }

    @FunctionalInterface
    public interface MetricListener {
        void onMetric(AIMetricRecord record);
    }
}
