# Changelog: FastAIMetrics

All notable changes to this project will be documented in this file.

## [0.1.0] - 2026-08-24
### Added
- **Lock-Free Telemetry Collector (`FastAIMetrics`)**: Microsecond-speed token, latency, and evaluation heuristic recording.
- **Dynamic Cost Engine (`AICostCalculator`)**: Zero-allocation pricing table supporting GPT-4o, Claude 3.5, Gemini 1.5, and LLaMA-3.
- **FastFileFormat Binary Streamer (`AIMetricsCodec`)**: Compact `.aimetrics` binary file format (Payload ID `0x0005`).
- **Interactive Showcase & JMH Benchmark Suite**: Measuring >389M pricing calculations/sec and >4M lock-free records/sec.
