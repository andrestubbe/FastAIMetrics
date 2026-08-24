package fastaimetrics;

import fastfileformat.BinaryHeader;
import fastfileformat.BinaryReader;
import fastfileformat.BinaryWriter;
import fastfileformat.FastFileFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-speed binary serializer and stream decoder for AI telemetry log files (.aimetrics).
 * Built on top of FastFileFormat and FastBinary VarInt compression.
 */
public final class AIMetricsCodec {
    /**
     * Payload type identifier for FastJava AI Metrics Logs (0x0005).
     */
    public static final short PAYLOAD_TYPE_AIMETRICS = 0x0005;

    private AIMetricsCodec() {}

    /**
     * Encodes a list of AIMetricRecord instances into a compressed FastFileFormat binary byte array.
     */
    public static byte[] encode(List<AIMetricRecord> records) {
        if (records == null || records.isEmpty()) {
            BinaryWriter finalWriter = FastFileFormat.binaryWriter(12);
            finalWriter.writeHeader(FastFileFormat.DEFAULT_MAGIC, FastFileFormat.DEFAULT_VERSION, PAYLOAD_TYPE_AIMETRICS, 0);
            return finalWriter.toByteArray();
        }

        BinaryWriter payloadWriter = FastFileFormat.binaryWriter(records.size() * 48);
        payloadWriter.writeVarInt(records.size());

        long baseTime = records.get(0).timestamp();
        payloadWriter.writeLong(baseTime);

        long lastTime = baseTime;
        for (AIMetricRecord rec : records) {
            long delta = rec.timestamp() - lastTime;
            lastTime = rec.timestamp();

            payloadWriter.writeVarLong(delta);
            payloadWriter.writeString(rec.traceId() != null ? rec.traceId() : "");
            payloadWriter.writeString(rec.model() != null ? rec.model() : "");
            payloadWriter.writeVarInt(rec.promptTokens());
            payloadWriter.writeVarInt(rec.completionTokens());
            payloadWriter.writeVarLong(rec.ttftNanos());
            payloadWriter.writeVarLong(rec.totalDurationNanos());
            payloadWriter.writeDouble(rec.estimatedCostUsd());
            payloadWriter.writeFloat(rec.evaluationScore());
        }

        byte[] payload = payloadWriter.toByteArray();

        BinaryWriter finalWriter = FastFileFormat.binaryWriter(12 + payload.length);
        finalWriter.writeHeader(
                FastFileFormat.DEFAULT_MAGIC,
                FastFileFormat.DEFAULT_VERSION,
                PAYLOAD_TYPE_AIMETRICS,
                payload.length
        );
        finalWriter.writeBytes(payload);
        return finalWriter.toByteArray();
    }

    /**
     * Decodes a .aimetrics binary payload into a list of AIMetricRecord instances.
     */
    public static List<AIMetricRecord> decode(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return Collections.emptyList();
        }

        BinaryReader reader = FastFileFormat.binaryReader(bytes);
        BinaryHeader header = reader.readHeader();

        if (header.getMagic() != FastFileFormat.DEFAULT_MAGIC) {
            throw new IllegalArgumentException("Invalid FastFileFormat magic header: " + Integer.toHexString(header.getMagic()));
        }
        if (header.getPayloadType() != PAYLOAD_TYPE_AIMETRICS) {
            throw new IllegalArgumentException("Unexpected payload type for AIMetrics: " + header.getPayloadType());
        }
        if (header.getPayloadLength() == 0) {
            return Collections.emptyList();
        }

        int count = reader.readVarInt();
        long currentTimestamp = reader.readLong();

        List<AIMetricRecord> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long delta = reader.readVarLong();
            currentTimestamp += delta;

            String traceId = reader.readString();
            String model = reader.readString();
            int promptTokens = reader.readVarInt();
            int completionTokens = reader.readVarInt();
            long ttftNanos = reader.readVarLong();
            long totalDurationNanos = reader.readVarLong();
            double estimatedCostUsd = reader.readDouble();
            float evaluationScore = reader.readFloat();

            list.add(new AIMetricRecord(
                    traceId,
                    model,
                    promptTokens,
                    completionTokens,
                    ttftNanos,
                    totalDurationNanos,
                    estimatedCostUsd,
                    evaluationScore,
                    currentTimestamp
            ));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Saves AI metric records directly to a .aimetrics file.
     */
    public static void writeToFile(Path path, List<AIMetricRecord> records) throws IOException {
        byte[] bytes = encode(records);
        Files.write(path, bytes);
    }

    /**
     * Reads AI metric records directly from a .aimetrics file.
     */
    public static List<AIMetricRecord> readFromFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return decode(bytes);
    }
}
