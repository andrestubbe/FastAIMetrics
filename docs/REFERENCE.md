# FastAIMetrics API Reference

## Core Classes

### 1. `fastaimetrics.FastAIMetrics`
* `public FastAIMetrics(Path outputDirectory, int bufferFlushThreshold)`: Creates collector instance with optional disk flushing.
* `public AIMetricRecord record(String traceId, String model, int promptTokens, int completionTokens, long ttftNanos, long totalDurationNanos, float evaluationScore)`: Records an execution and computes pricing.
* `public void addListener(MetricListener listener)`: Registers event callback.
* `public long getTotalPromptTokens()` / `getTotalCompletionTokens()` / `getTotalTokens()`: Returns atomic token counts.
* `public double getTotalCostUsd()`: Returns accumulated total USD cost.
* `public double getAverageLatencyMillis()`: Computes global average execution latency.
* `public void flush()`: Writes buffered records to `.aimetrics` file.

### 2. `fastaimetrics.AICostCalculator`
* `public static void registerModel(String model, double inPricePerM, double outPricePerM)`: Registers custom model pricing.
* `public static double calculateCost(String model, int promptTokens, int completionTokens)`: Zero-allocation pricing calculation in USD.

### 3. `fastaimetrics.AIMetricsCodec`
* `public static byte[] encode(List<AIMetricRecord> records)`: FastFileFormat binary serializer.
* `public static List<AIMetricRecord> decode(byte[] bytes)`: FastFileFormat binary deserializer.
* `public static void writeToFile(Path path, List<AIMetricRecord> records)`: Saves records directly to file.
* `public static List<AIMetricRecord> readFromFile(Path path)`: Reads records directly from file.
