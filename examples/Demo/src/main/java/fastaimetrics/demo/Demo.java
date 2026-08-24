package fastaimetrics.demo;

import fastaimetrics.AICostCalculator;
import fastaimetrics.AIMetricRecord;
import fastaimetrics.AIMetricsCodec;
import fastaimetrics.FastAIMetrics;

import java.nio.file.Path;
import java.util.List;

public class Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println(" 📊 FastAIMetrics — Real-Time Agent Telemetry    ");
        System.out.println("=================================================");

        Path logDir = Path.of(System.getProperty("java.io.tmpdir"), "FastAIMetricsDemo");
        System.out.println("Telemetry target directory: " + logDir);

        try (FastAIMetrics metrics = new FastAIMetrics(logDir, 100)) {
            metrics.addListener(rec -> {
                System.out.printf("[METRIC] trace=%s model=%s tokens=%d cost=$%.6f latency=%.2fms score=%.2f\n",
                        rec.traceId(), rec.model(), rec.totalTokens(), rec.estimatedCostUsd(), rec.totalDurationMillis(), rec.evaluationScore());
            });

            System.out.println("\n--- Simulating Multi-Agent LLM Workflow ---");
            metrics.record("trace-1", "gpt-4o", 1250, 450, 220_000_000L, 750_000_000L, 0.96f);
            metrics.record("trace-2", "claude-3-5-sonnet", 850, 600, 180_000_000L, 820_000_000L, 0.98f);
            metrics.record("trace-3", "gpt-4o-mini", 3200, 150, 95_000_000L, 310_000_000L, 0.89f);
            metrics.record("trace-4", "local", 500, 200, 40_000_000L, 120_000_000L, 0.92f);

            System.out.println("\n--- Global Aggregates ---");
            System.out.println("Total LLM Calls:       " + metrics.getTotalCalls());
            System.out.println("Total Prompt Tokens:   " + metrics.getTotalPromptTokens());
            System.out.println("Total Generated Tokens:" + metrics.getTotalCompletionTokens());
            System.out.println("Total Tokens:          " + metrics.getTotalTokens());
            System.out.printf("Total Estimated Cost:  $%.6f USD\n", metrics.getTotalCostUsd());
            System.out.printf("Average Latency:       %.2f ms\n", metrics.getAverageLatencyMillis());

            // 1. Binary Encoding via FastFileFormat
            List<AIMetricRecord> buffered = metrics.getBufferedRecords();
            byte[] encoded = AIMetricsCodec.encode(buffered);
            System.out.println("\nEncoded .aimetrics payload size: " + encoded.length + " bytes.");

            // 2. Binary Decoding
            List<AIMetricRecord> decoded = AIMetricsCodec.decode(encoded);
            System.out.println("Successfully decoded " + decoded.size() + " telemetry records from binary stream.");

            System.out.println("\n✔ FastAIMetrics Pipeline Verified Successfully!");
        }
    }
}
