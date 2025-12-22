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
import java.util.Arrays;
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
        BufferedImage image = javax.imageio.ImageIO.read(imageFile);
        int width = image.getWidth();
        int height = image.getHeight();

        BlhoFileDataV2 fileData =
                createHashStructure(blocks, imageFile, width, height);

        String outputPath = imageFile.getAbsolutePath() + ".blho";
        File outputFile = new File(outputPath);

        writeBlhoFile(outputFile, fileData);

        log.info("""
                        BLHO v2 written:
                          total blocks: {}
                          unique Y: {}
                          unique U: {}
                          unique V: {}
                          size: {} KB
                        """,
                fileData.totalBlocks(),
                fileData.getUniqueYHashes().size(),
                fileData.getUniqueUHashes().size(),
                fileData.getUniqueVHashes().size(),
                outputFile.length() / 1024
        );
    }

    /**
     * Структура для хранения данных .blho файла
     */
    @Getter
    private static class BlhoFileDataV2 {

        String originalFileName;
        int width;
        int height;

        List<byte[]> uniqueYHashes;
        List<byte[]> uniqueUHashes;
        List<byte[]> uniqueVHashes;

        List<Integer> yPositionMap;
        List<Integer> uPositionMap;
        List<Integer> vPositionMap;

        int totalBlocks() {
            return yPositionMap.size();
        }
    }


    /**
     * Дублирует блоки и создает структуру данных для файла
     */
    private BlhoFileDataV2 createHashStructure(
            List<RctBlock> blocks,
            File imageFile,
            int width,
            int height
    ) {
        Map<ByteBuffer, Integer> yIndex = new LinkedHashMap<>();
        Map<ByteBuffer, Integer> uIndex = new LinkedHashMap<>();
        Map<ByteBuffer, Integer> vIndex = new LinkedHashMap<>();

        List<byte[]> uniqueY = new ArrayList<>();
        List<byte[]> uniqueU = new ArrayList<>();
        List<byte[]> uniqueV = new ArrayList<>();

        List<Integer> yPos = new ArrayList<>();
        List<Integer> uPos = new ArrayList<>();
        List<Integer> vPos = new ArrayList<>();

        for (RctBlock block : blocks) {
            byte[] yHash = HashUtils.sha256Bytes(block.y());
            byte[] uHash = HashUtils.sha256Bytes(block.uPacked());
            byte[] vHash = HashUtils.sha256Bytes(block.vPacked());

            int yIdx = yIndex.computeIfAbsent(
                    ByteBuffer.wrap(Arrays.copyOf(yHash, yHash.length)).asReadOnlyBuffer(),
                    _ -> {
                        uniqueY.add(yHash);
                        return uniqueY.size() - 1;
                    });

            int uIdx = uIndex.computeIfAbsent(
                    ByteBuffer.wrap(Arrays.copyOf(uHash, uHash.length)).asReadOnlyBuffer(),
                    _ -> {
                        uniqueU.add(uHash);
                        return uniqueU.size() - 1;
                    });

            int vIdx = vIndex.computeIfAbsent(
                    ByteBuffer.wrap(Arrays.copyOf(vHash, vHash.length)).asReadOnlyBuffer(),
                    _ -> {
                        uniqueV.add(vHash);
                        return uniqueV.size() - 1;
                    });

            yPos.add(yIdx);
            uPos.add(uIdx);
            vPos.add(vIdx);
        }

        BlhoFileDataV2 data = new BlhoFileDataV2();
        data.originalFileName = imageFile.getName();
        data.width = width;
        data.height = height;
        data.uniqueYHashes = uniqueY;
        data.uniqueUHashes = uniqueU;
        data.uniqueVHashes = uniqueV;
        data.yPositionMap = yPos;
        data.uPositionMap = uPos;
        data.vPositionMap = vPos;

        return data;
    }

    /**
     * Записывает .blho файл в бинарном формате
     */
    private void writeBlhoFile(File outputFile, BlhoFileDataV2 fileData) throws Exception {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(outputFile.toPath())))) {

            writeHeader(dos);
            writeMetadata(dos, fileData);

            writeHashList(dos, fileData.getUniqueYHashes());
            writeHashList(dos, fileData.getUniqueUHashes());
            writeHashList(dos, fileData.getUniqueVHashes());

            writePositionMap(dos, fileData.getYPositionMap());
            writePositionMap(dos, fileData.getUPositionMap());
            writePositionMap(dos, fileData.getVPositionMap());
        }
    }

    /**
     * Записывает заголовок файла
     */
    private void writeHeader(DataOutputStream dos) throws IOException {
        dos.write("BLHO".getBytes(StandardCharsets.US_ASCII));
        dos.writeByte(2);
    }

    /**
     * Записывает метаданные в формате JSON
     */
    private void writeMetadata(DataOutputStream dos, BlhoFileDataV2 fileData) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("format", "BLHO");
        metadata.put("version", "2.0");
        metadata.put("file", fileData.getOriginalFileName());
        metadata.put("width", fileData.getWidth());
        metadata.put("height", fileData.getHeight());
        metadata.put("total_blocks", fileData.totalBlocks());
        metadata.put("unique_y_blocks", fileData.getUniqueYHashes().size());
        metadata.put("unique_u_blocks", fileData.getUniqueUHashes().size());
        metadata.put("unique_v_blocks", fileData.getUniqueVHashes().size());

        String json = objectMapper.writeValueAsString(metadata);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // 4 bytes metadata_length
        dos.writeInt(jsonBytes.length);
        // N bytes metadata_json
        dos.write(jsonBytes);
    }

    private void writeHashList(DataOutputStream dos, List<byte[]> hashes)
            throws IOException {

        dos.writeInt(hashes.size());
        for (byte[] hash : hashes) {
            if (hash.length != 32) {
                throw new IllegalStateException("Invalid SHA-256 hash");
            }
            dos.write(hash);
        }
    }

    private void writePositionMap(DataOutputStream dos, List<Integer> map)
            throws IOException {

        dos.writeInt(map.size());
        for (int idx : map) {
            dos.writeInt(idx);
        }
    }
}