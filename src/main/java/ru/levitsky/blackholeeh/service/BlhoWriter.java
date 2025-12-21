package ru.levitsky.blackholeeh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.levitsky.blackholeeh.model.RctBlock;
import ru.levitsky.blackholeeh.util.HashUtils;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BlhoWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Записывает .blho файл для указанного изображения
     */
    public void writeBlho(File imageFile, List<RctBlock> blocks) throws Exception {
        long startTime = System.nanoTime();

        BufferedImage image = javax.imageio.ImageIO.read(imageFile);
        int width = image.getWidth();
        int height = image.getHeight();

        BlhoFileData fileData = deduplicateAndCreateStructure(blocks, imageFile.getName(), width, height);

        String outputPath = imageFile.getAbsolutePath() + ".blho";
        File outputFile = new File(outputPath);

        writeBlhoFile(outputFile, fileData);

        long timeMs = (System.nanoTime() - startTime) / 1_000_000;
        long outputSize = outputFile.length();

        log.info("Created .blho file: {} ({} blocks -> {} unique, {} bytes, {} ms)",
                outputFile.getName(),
                fileData.getTotalBlocks(),
                fileData.getUniqueBlocks().size(),
                outputSize,
                timeMs);
    }

    /**
     * Структура для хранения данных .blho файла
     */
    @Getter
    private static class BlhoFileData {
        private final String originalFileName;
        private final int width;
        private final int height;
        private final int totalBlocks;
        private final List<RctBlock> uniqueBlocks;
        private final List<Integer> positionMap;

        public BlhoFileData(String originalFileName, int width, int height,
                            List<RctBlock> uniqueBlocks,
                            List<Integer> positionMap) {
            this.originalFileName = originalFileName;
            this.width = width;
            this.height = height;
            this.totalBlocks = positionMap.size();
            this.uniqueBlocks = uniqueBlocks;
            this.positionMap = positionMap;
        }

    }

    /**
     * Дублирует блоки и создает структуру данных для файла
     */
    private BlhoFileData deduplicateAndCreateStructure(List<RctBlock> blocks,
                                                       String fileName, int width, int height) {
        Map<String, RctBlock> uniqueBlocksMap = new LinkedHashMap<>();
        Map<String, Integer> blockIndexMap = new HashMap<>();
        List<Integer> positionMap = new ArrayList<>();

        for (RctBlock block : blocks) {
            String blockHash = computeBlockHash(block);

            if (!uniqueBlocksMap.containsKey(blockHash)) {
                int index = uniqueBlocksMap.size();
                uniqueBlocksMap.put(blockHash, block);
                blockIndexMap.put(blockHash, index);
            }

            positionMap.add(blockIndexMap.get(blockHash));
        }

        return new BlhoFileData(
                fileName, width, height,
                new ArrayList<>(uniqueBlocksMap.values()),
                positionMap
        );
    }

    /**
     * Вычисляет хеш для всего блока (Y+U+V)
     */
    private String computeBlockHash(RctBlock block) {
        byte[] combined = concatenateArrays(
                block.y(),
                block.uPacked(),
                block.vPacked()
        );
        return HashUtils.sha256WithLength(combined);
    }

    /**
     * Объединяет несколько массивов байтов в один
     */
    private byte[] concatenateArrays(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        for (byte[] array : arrays) {
            buffer.put(array);
        }

        return buffer.array();
    }

    /**
     * Записывает .blho файл в бинарном формате
     */
    private void writeBlhoFile(File outputFile, BlhoFileData fileData) throws Exception {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(outputFile.toPath())))) {

            writeHeader(dos);
            writeMetadata(dos, fileData);
            writeUniqueBlocks(dos, fileData.getUniqueBlocks());
            writePositionMap(dos, fileData.getPositionMap());
        }
    }

    /**
     * Записывает заголовок файла
     */
    private void writeHeader(DataOutputStream dos) throws IOException {
        dos.write("BLHO".getBytes(StandardCharsets.UTF_8));
        dos.writeByte(1);
    }

    /**
     * Записывает метаданные в формате JSON
     */
    private void writeMetadata(DataOutputStream dos, BlhoFileData fileData) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("format", "BLHO");
        metadata.put("version", "1.0");
        metadata.put("file", fileData.getOriginalFileName());
        metadata.put("width", fileData.getWidth());
        metadata.put("height", fileData.getHeight());
        metadata.put("block_size", 8);
        metadata.put("total_blocks", fileData.getTotalBlocks());
        metadata.put("unique_blocks", fileData.getUniqueBlocks().size());

        String json = objectMapper.writeValueAsString(metadata);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // 4 bytes metadata_length
        dos.writeInt(jsonBytes.length);
        // N bytes metadata_json
        dos.write(jsonBytes);
    }

    /**
     * Записывает уникальные блоки
     */
    private void writeUniqueBlocks(DataOutputStream dos, List<RctBlock> uniqueBlocks) throws IOException {
        // 4 bytes unique_blocks_count
        dos.writeInt(uniqueBlocks.size());

        for (RctBlock block : uniqueBlocks) {
            // Y component
            writeByteArrayWithLength(dos, block.y());

            // U component (packed)
            writeByteArrayWithLength(dos, block.uPacked());

            // V component (packed)
            writeByteArrayWithLength(dos, block.vPacked());
        }
    }

    /**
     * Записывает массив байтов с предварительной длиной (2 байта)
     */
    private void writeByteArrayWithLength(DataOutputStream dos, byte[] data) throws IOException {
        // 2 bytes length
        dos.writeShort(data.length);
        // N bytes data
        dos.write(data);
    }

    /**
     * Записывает карту позиций
     */
    private void writePositionMap(DataOutputStream dos, List<Integer> positionMap) throws IOException {
        // 4 bytes total_positions
        dos.writeInt(positionMap.size());

        for (Integer index : positionMap) {
            // 4 bytes unique_block_index
            dos.writeInt(index);
        }
    }
}