package fastaimetrics.benchmark;

import fastaimetrics.AICostCalculator;
import fastaimetrics.AIMetricRecord;
import fastaimetrics.AIMetricsCodec;
import fastaimetrics.FastAIMetrics;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class Benchmark {

    private FastAIMetrics metrics;
    private List<AIMetricRecord> sampleRecords;
    private byte[] sampleBinary;

    @Setup
    public void setup() {
        metrics = new FastAIMetrics(null, 100_000);
        sampleRecords = new ArrayList<>(1000);
        long baseTime = 1770000000000L;

        for (int i = 0; i < 1000; i++) {
            sampleRecords.add(new AIMetricRecord(
                    "trace-" + i,
                    "gpt-4o",
                    1200 + (i % 50),
                    350 + (i % 20),
                    180_000_000L,
                    650_000_000L,
                    0.0065,
                    0.95f,
                    baseTime + (i * 100L)
            ));
        }
        sampleBinary = AIMetricsCodec.encode(sampleRecords);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public AIMetricRecord benchmarkRecordMetric() {
        return metrics.record("trace-bench", "gpt-4o", 1200, 350, 180_000_000L, 650_000_000L, 0.95f);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public double benchmarkCostCalculation() {
        return AICostCalculator.calculateCost("gpt-4o", 1500, 400);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public byte[] benchmarkEncode1000Metrics() {
        return AIMetricsCodec.encode(sampleRecords);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public List<AIMetricRecord> benchmarkDecode1000Metrics() {
        return AIMetricsCodec.decode(sampleBinary);
    }
}
