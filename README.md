# FastAIMetrics 0.1.0 [ALPHA] — Ultra-Fast Lock-Free AI Telemetry, Cost Engine & Evaluation Streamer

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastAIMetrics/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Cross--Platform-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastAIMetrics)

---

**⚡ Ultra-fast lock-free LLM token tracker, real-time cost calculator, streaming latency profiler, and `.aimetrics` dual-format binary telemetry engine for Java.**

**FastAIMetrics** provides zero-allocation, nanosecond-speed telemetry for autonomous agents, LLM client pipelines, and RAG architectures. It tracks prompt/completion tokens, Time-to-First-Token (TTFT), execution latencies, dynamic model pricing, and evaluation heuristic scores without external servers, OpenTelemetry bloat, or Docker overhead.

---

## Quick Start

```java
import fastaimetrics.*;
import java.nio.file.Path;
import java.util.List;

public class Demo {
    public static void main(String[] args) throws Exception {
        Path logDir = Path.of("logs/metrics");

        // 1. Live lock-free telemetry recording
        try (FastAIMetrics metrics = new FastAIMetrics(logDir, 5000)) {
            metrics.addListener(rec -> {
                System.out.printf("Trace %s [%s]: tokens=%d cost=$%.6f latency=%.2fms\n",
                        rec.traceId(), rec.model(), rec.totalTokens(), rec.estimatedCostUsd(), rec.totalDurationMillis());
            });

            // Record LLM call (e.g. GPT-4o, 1250 prompt tokens, 450 completion tokens)
            metrics.record("trace-1", "gpt-4o", 1250, 450, 220_000_000L, 750_000_000L, 0.96f);
            metrics.stop(); // Flushes to timestamped .aimetrics
        }

        // 2. High-speed FastFileFormat codec
        Path sessionFile = logDir.resolve("session.aimetrics");
        List<AIMetricRecord> records = AIMetricsCodec.readFromFile(sessionFile);
        System.out.println("Loaded " + records.size() + " telemetry records.");
    }
}
```

---

## Key Features

- **⚡ Lock-Free Aggregate Tracking** — High-concurrency `LongAdder` & `DoubleAdder` counters with zero lock contention.
- **💵 Zero-Allocation Cost Engine** — Instant dynamic pricing matrix (`AICostCalculator`) supporting GPT-4o, Claude 3.5 Sonnet, Gemini 1.5 Pro, LLaMA-3, and local models.
- **⏱️ Microsecond Latency Profiling** — Tracks total execution time, TTFT (Time-to-First-Token), and generation token velocity (Tokens/sec).
- **📦 FastFileFormat `.aimetrics` Compression** — Compact VarInt delta-timestamped binary serialization (Payload ID `0x0005`).
- **🛡️ Embedded & Air-Gapped** — 100% in-process Java 17+, zero external database or cloud monitoring dependencies.

---

## Real-World Scenarios

- **🤖 Multi-Agent Loop Telemetry** — Tracking cumulative token spend and stopping runaway recursive agent loops in `FastAIAgent`.
- **⚡ Provider SLA & Routing** — Measuring TTFT and token generation velocity to dynamically route prompts across multi-LLM clusters.
- **📊 RAG Quality & Cost Audit** — Correlating retrieval precision evaluation scores with token consumption in `FastAIRag`.
- **📑 Autonomous Budget Enforcement** — Real-time cost threshold triggers and multi-tenant billing calculations.

---

## Performance Benchmarks

FastAIMetrics is profiled using **JMH** to guarantee nanosecond-level instrumentation overhead.

| Benchmark Operation | Score (ops/ms) | Throughput | Memory Overhead |
|---|---|---|---|
| **Cost Matrix Calculation (`AICostCalculator`)** | **~389,000 ops/ms** | **> 389 Million ops/sec** | **0 bytes allocation** |
| **Lock-Free Event Recording (`FastAIMetrics`)** | **~4,000 ops/ms** | **> 4.0 Million events/sec** | **Atomic Ringbuffer** |
| **Binary Stream Decoding (`.aimetrics`)** | **~14,000 ops/ms** | **> 14.0 Million events/sec** | **Zero-Copy Streaming** |
| **Binary Stream Encoding (`.aimetrics`)** | **~9,400 ops/ms** | **> 9.4 Million events/sec** | **VarInt Delta Buffer** |

*Run the benchmarks locally:* `.\run-benchmark.bat`

---

## API Quick Reference

| Method / Class | Description |
|---|---|
| `new FastAIMetrics(path, threshold)` | Creates a collector flushing every N records into timestamped `.aimetrics` files. |
| `metrics.record(traceId, model, promptTokens, ...)` | Records an LLM execution, calculates cost, and updates global aggregates. |
| `metrics.getTotalCostUsd()` / `getTotalTokens()` | Returns atomic global spend and token totals. |
| `AICostCalculator.calculateCost(model, in, out)` | Computes estimated cost in USD with nanosecond speed. |
| `AIMetricsCodec.encode(records)` | Serializes metric records into compressed FastFileFormat binary byte array. |
| `AIMetricsCodec.decode(bytes)` | Deserializes `.aimetrics` binary bytes back into `List<AIMetricRecord>`. |

---

## Technical Examples & Hero Demos

| Case | Java Example | Launcher | Description |
|---|---|---|---|
| **Live Telemetry & Aggregation Demo** | [Demo.java](examples/Demo/src/main/java/fastaimetrics/demo/Demo.java) | `run-demo.bat` | Multi-agent LLM telemetry simulation, aggregate statistics, and `.aimetrics` binary streaming. |
| **JMH Microbenchmark Suite** | [Benchmark.java](examples/Benchmark/src/main/java/fastaimetrics/benchmark/Benchmark.java) | `run-benchmark.bat` | High-throughput cost calculation, lock-free recording, and binary codec benchmarks. |

---

## Installation

### Option 1: Maven (JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastAIMetrics</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastFileFormat</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastBinary</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastcore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastAIMetrics:0.1.0'
    implementation 'com.github.andrestubbe:FastFileFormat:0.1.0'
    implementation 'com.github.andrestubbe:FastBinary:0.1.0'
    implementation 'com.github.andrestubbe:fastcore:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest JARs directly to add them to your classpath:

1. 📊 **[FastAIMetrics-0.1.0.jar](https://github.com/andrestubbe/FastAIMetrics/releases/download/0.1.0/FastAIMetrics-0.1.0.jar)** (AI Metrics & Cost Telemetry Engine)
2. 📄 **[FastFileFormat-0.1.0.jar](https://github.com/andrestubbe/FastFileFormat/releases/download/0.1.0/FastFileFormat-0.1.0.jar)** (Dual Binary & Text File Format)
3. ⚡ **[FastBinary-0.1.0.jar](https://github.com/andrestubbe/FastBinary/releases/download/0.1.0/FastBinary-0.1.0.jar)** (VarInt & Binary Packing)
4. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (Foundation Library)

---

## Documentation

* **[REFERENCE.md](docs/REFERENCE.md)**: Full API reference and method signatures.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Architectural design principles and in-process zero-overhead telemetry.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Release history and version notes.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future milestones and planned features.
* **[COMPILE.md](docs/COMPILE.md)**: Instructions for compiling from source.

---

## License

MIT License. See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastAI](https://github.com/andrestubbe/FastAI) — Unified provider routing & LLM client layer
- [FastAIAgent](https://github.com/andrestubbe/FastAIAgent) — Autonomous agent logic, tools & planning
- [FastAIState](https://github.com/andrestubbe/FastAIState) — Lock-free shared agent state & blackboard memory
- [FastAIRag](https://github.com/andrestubbe/FastAIRag) — High-speed retrieval augmented generation pipeline
- [FastFileFormat](https://github.com/andrestubbe/FastFileFormat) — Universal dual-format binary & text document engine

---

**Part of the FastJava Ecosystem** — *Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋*
