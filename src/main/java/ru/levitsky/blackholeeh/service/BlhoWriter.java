package ru.levitsky.blackholeeh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.levitsky.blackholeeh.model.BlhoFileDataV2;
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
     * Creates and writes a .blho file for the specified image.<br>
     * The BLHO format stores only SHA-256 hashes of Y/U/V blocks and position maps,
     * which significantly reduces the file size compared to the original image,
     * while preserving the ability to fully restore the original image (lossless).<br>
     * Execution process:<br>
     * - Reads the image into a BufferedImage<br>
     * - Creates a block structure with hashes (Y, U, V) and position maps<br>
     * - Writes the .blho file in binary format with header, metadata, hashes, and position maps<br>
     * - Logs statistics: total number of blocks, unique blocks per component, and file size<br>
     *
     * @param imageFile the source JPG/JPEG image file
     * @param blocks    the list of RCT blocks extracted from the image
     * @throws Exception if an error occurs while reading the image, creating the structure, or writing the file
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
     * Дублирует блоки и создает структуру данных для файла
     */
    private BlhoFileDataV2 createHashStructure(List<RctBlock> blocks, File imageFile, int width, int height) {
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

        return new BlhoFileDataV2(imageFile.getName(), width, height, uniqueY, uniqueU, uniqueV, yPos, uPos, vPos);
    }

    /**
     * Writes a complete .blho file in binary format.
     * <p>
     * The method writes the file sequentially in the following order:
     * <ol>
     *   <li>File header (format identifier and version)</li>
     *   <li>Metadata block encoded as JSON</li>
     *   <li>Lists of unique SHA-256 hashes for Y, U, and V blocks</li>
     *   <li>Position maps for Y, U, and V blocks</li>
     * </ol>
     * <p>
     * This structure allows the original image to be reconstructed in a fully
     * lossless manner by combining the position maps with externally stored
     * block data.
     *
     * @param outputFile the target .blho file to be written
     * @param fileData   the structured BLHO data containing metadata, hashes,
     *                   and position maps
     * @throws Exception if an error occurs while creating the output stream
     *                   or writing any part of the file
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
     * Writes the header of the .blho file in binary format.<br>
     * The header consists of:<br>
     * - 4 ASCII bytes representing the string "BLHO"<br>
     * - 1 byte for the file format version (currently 2)<br>
     *
     * @param dos {@link DataOutputStream}the output stream to write the binary .blho data
     * @throws IOException if an error occurs while writing to the stream
     */
    private void writeHeader(DataOutputStream dos) throws IOException {
        dos.write("BLHO".getBytes(StandardCharsets.US_ASCII));
        dos.writeByte(2);
    }

    /**
     * Writes the metadata of the .blho file in binary format.<br>
     * Metadata includes information about the format, version, original file name,
     * image dimensions, total number of blocks, and counts of unique blocks
     * for each component (Y, U, V).<br>
     * The metadata is serialized as JSON and written with a 4-byte integer
     * prefix indicating the length of the JSON.
     *
     * @param dos      the output stream to write the binary .blho data
     * @param fileData the BLHO v2 data structure containing block and image information
     * @throws IOException if an error occurs while writing data to the stream
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

        dos.writeInt(jsonBytes.length);
        dos.write(jsonBytes);
    }

    /**
     * Writes a list of SHA-256 hashes to the .blho file in binary format.<br>
     * Each hash must be exactly 32 bytes.<br>
     * The method first writes the number of hashes as a 4-byte integer,
     * followed by each 32-byte hash in the order they appear in the list.<br>
     *
     * @param dos    the output stream to write the binary .blho data
     * @param hashes the list of SHA-256 hashes to write
     * @throws IOException           if an error occurs while writing to the stream
     * @throws IllegalStateException if any hash does not have exactly 32 bytes
     */
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

    /**
     * Writes a position map to the .blho file in binary format.<br>
     * A position map stores the indices of blocks to reconstruct the original image layout.<br>
     * The method first writes the number of entries as a 4-byte integer,
     * followed by each index as a 4-byte integer in the order they appear in the list.<br>
     *
     * @param dos the output stream to write the binary .blho data
     * @param map the list of block indices representing the position map
     * @throws IOException if an error occurs while writing to the stream
     */
    private void writePositionMap(DataOutputStream dos, List<Integer> map)
            throws IOException {

        dos.writeInt(map.size());
        for (int idx : map) {
            dos.writeInt(idx);
        }
    }
}