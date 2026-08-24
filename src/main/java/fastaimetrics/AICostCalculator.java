package fastaimetrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, zero-allocation pricing matrix and cost calculator for LLMs.
 */
public final class AICostCalculator {

    public record ModelPricing(double inputPricePerMillion, double outputPricePerMillion) {}

    private static final Map<String, ModelPricing> PRICING_TABLE = new ConcurrentHashMap<>();

    static {
        // Standard baseline pricing (USD per 1M tokens)
        PRICING_TABLE.put("gpt-4o", new ModelPricing(2.50, 10.00));
        PRICING_TABLE.put("gpt-4o-mini", new ModelPricing(0.15, 0.60));
        PRICING_TABLE.put("claude-3-5-sonnet", new ModelPricing(3.00, 15.00));
        PRICING_TABLE.put("claude-3-haiku", new ModelPricing(0.25, 1.25));
        PRICING_TABLE.put("gemini-1.5-pro", new ModelPricing(3.50, 10.50));
        PRICING_TABLE.put("gemini-1.5-flash", new ModelPricing(0.35, 1.05));
        PRICING_TABLE.put("llama-3-70b", new ModelPricing(0.50, 0.90));
        PRICING_TABLE.put("llama-3-8b", new ModelPricing(0.10, 0.10));
        PRICING_TABLE.put("local", new ModelPricing(0.00, 0.00));
    }

    private AICostCalculator() {}

    /**
     * Registers or updates pricing for a specific model.
     *
     * @param model Model identifier.
     * @param inputPricePerMillion Price in USD per 1M input tokens.
     * @param outputPricePerMillion Price in USD per 1M output tokens.
     */
    public static void registerModel(String model, double inputPricePerMillion, double outputPricePerMillion) {
        PRICING_TABLE.put(model.toLowerCase(), new ModelPricing(inputPricePerMillion, outputPricePerMillion));
    }

    /**
     * Calculates estimated cost in USD for a given token usage.
     */
    public static double calculateCost(String model, int promptTokens, int completionTokens) {
        if (model == null) return 0.0;
        ModelPricing pricing = PRICING_TABLE.get(model.toLowerCase());
        if (pricing == null) {
            // Default fallback assumption ($1 per 1M input, $3 per 1M output)
            return (promptTokens * 1.0 / 1_000_000.0) + (completionTokens * 3.0 / 1_000_000.0);
        }
        return (promptTokens * pricing.inputPricePerMillion / 1_000_000.0) +
                (completionTokens * pricing.outputPricePerMillion / 1_000_000.0);
    }
}
