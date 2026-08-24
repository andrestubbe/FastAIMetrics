package fastaimetrics;

/**
 * Immutable telemetry record for an AI model execution, tool call, or reasoning step.
 *
 * @param traceId Unique trace identifier string.
 * @param model Model identifier string (e.g. "gpt-4o", "claude-3-5-sonnet", "llama-3-70b").
 * @param promptTokens Number of input prompt tokens.
 * @param completionTokens Number of generated completion tokens.
 * @param ttftNanos Time-to-First-Token in nanoseconds.
 * @param totalDurationNanos Total execution duration in nanoseconds.
 * @param estimatedCostUsd Estimated cost in US Dollars (computed from pricing matrix).
 * @param evaluationScore Quality/alignment heuristic score (0.0f - 1.0f).
 * @param timestamp Epoch timestamp in milliseconds.
 */
public record AIMetricRecord(
        String traceId,
        String model,
        int promptTokens,
        int completionTokens,
        long ttftNanos,
        long totalDurationNanos,
        double estimatedCostUsd,
        float evaluationScore,
        long timestamp
) {
    public int totalTokens() {
        return promptTokens + completionTokens;
    }

    public double totalDurationMillis() {
        return totalDurationNanos / 1_000_000.0;
    }

    public double ttftMillis() {
        return ttftNanos / 1_000_000.0;
    }

    public double tokensPerSecond() {
        if (totalDurationNanos <= 0 || completionTokens <= 0) return 0.0;
        return (completionTokens / (double) totalDurationNanos) * 1_000_000_000.0;
    }
}
