package fastaimetrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FastAIMetricsTest {

    @Test
    public void testCodecEncodingAndDecoding() {
        AIMetricRecord r1 = new AIMetricRecord(
                "trace-1", "gpt-4o", 1500, 300, 250_000_000L, 800_000_000L, 0.00675, 0.95f, 1770000000000L
        );
        AIMetricRecord r2 = new AIMetricRecord(
                "trace-2", "claude-3-5-sonnet", 800, 450, 180_000_000L, 600_000_000L, 0.00915, 0.98f, 1770000001000L
        );

        byte[] encoded = AIMetricsCodec.encode(List.of(r1, r2));
        assertNotNull(encoded);
        assertTrue(encoded.length >= 12);

        List<AIMetricRecord> decoded = AIMetricsCodec.decode(encoded);
        assertEquals(2, decoded.size());

        assertEquals("trace-1", decoded.get(0).traceId());
        assertEquals("gpt-4o", decoded.get(0).model());
        assertEquals(1500, decoded.get(0).promptTokens());
        assertEquals(300, decoded.get(0).completionTokens());
        assertEquals(0.95f, decoded.get(0).evaluationScore(), 0.001f);

        assertEquals("trace-2", decoded.get(1).traceId());
        assertEquals("claude-3-5-sonnet", decoded.get(1).model());
    }

    @Test
    public void testCostCalculation() {
        // GPT-4o: $2.50 / 1M prompt, $10.00 / 1M completion
        // 1000 prompt tokens = $0.0025, 1000 completion tokens = $0.0100 -> Total = $0.0125
        double cost = AICostCalculator.calculateCost("gpt-4o", 1000, 1000);
        assertEquals(0.0125, cost, 0.00001);
    }

    @Test
    public void testMetricsCollectorAndAggregates(@TempDir Path tempDir) throws IOException {
        try (FastAIMetrics metrics = new FastAIMetrics(tempDir, 10)) {
            metrics.record("t1", "gpt-4o", 100, 50, 100_000_000L, 200_000_000L, 0.9f);
            metrics.record("t2", "gpt-4o", 200, 100, 150_000_000L, 300_000_000L, 0.8f);

            assertEquals(300, metrics.getTotalPromptTokens());
            assertEquals(150, metrics.getTotalCompletionTokens());
            assertEquals(450, metrics.getTotalTokens());
            assertEquals(2, metrics.getTotalCalls());
            assertTrue(metrics.getTotalCostUsd() > 0.0);
            assertEquals(250.0, metrics.getAverageLatencyMillis(), 0.01);
        }
    }
}
