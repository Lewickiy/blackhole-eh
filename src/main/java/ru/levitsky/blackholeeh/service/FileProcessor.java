package ru.levitsky.blackholeeh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.levitsky.blackholeeh.dto.BlockDto;
import ru.levitsky.blackholeeh.enumeration.BlockType;
import ru.levitsky.blackholeeh.util.HashUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessor {

    private final BlockClient blockClient;

    /**
     * Process all JPG/JPEG images in the directory
     */
    public void processDirectory(String dirPath) throws IOException {
        Path dir = Path.of(dirPath);
        if (!Files.isDirectory(dir)) {
            log.error("'{}' is not a directory", dir);
            return;
        }

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.toString().toLowerCase().endsWith(".jpg") ||
                            p.toString().toLowerCase().endsWith(".jpeg"))
                    .forEach(p -> {
                        try {
                            processFile(p.toFile());
                        } catch (Exception e) {
                            log.error("Error processing {}: {}", p.getFileName(), e.getMessage());
                        }
                    });
        }
    }

    /**
     * Split file into RCT blocks, deduplicate and upload missing blocks
     */
    private void processFile(File file) throws Exception {
        log.info("Processing file: {}", file.getName());

        List<BlockSplitter.RctBlock> blocks = BlockSplitter.splitIntoRctBlocks(file);

        // Deduplicated maps for Y, U, V blocks
        Map<String, byte[]> yMap = new LinkedHashMap<>();
        Map<String, byte[]> uMap = new LinkedHashMap<>();
        Map<String, byte[]> vMap = new LinkedHashMap<>();

        for (BlockSplitter.RctBlock block : blocks) {
            String yHash = HashUtils.sha256WithLength(block.y());
            String uHash = HashUtils.sha256WithLength(block.uPacked());
            String vHash = HashUtils.sha256WithLength(block.vPacked());

            yMap.putIfAbsent(yHash, block.y());
            uMap.putIfAbsent(uHash, block.uPacked());
            vMap.putIfAbsent(vHash, block.vPacked());
        }

        uploadMissingBlocks(yMap, BlockType.LUMA);
        uploadMissingBlocks(uMap, BlockType.CHROMA_CB);
        uploadMissingBlocks(vMap, BlockType.CHROMA_CR);

        log.info("File '{}' processed: {} Y blocks, {} U blocks, {} V blocks (unique)",
                file.getName(), yMap.size(), uMap.size(), vMap.size());
    }

    /**
     * Upload missing blocks to the server
     */
    private void uploadMissingBlocks(Map<String, byte[]> blockMap, BlockType type) {
        if (blockMap.isEmpty()) {
            log.info("No {} blocks to upload", type);
            return;
        }

        List<String> hashes = new ArrayList<>(blockMap.keySet());
        List<String> missing = blockClient.checkMissingBlocks(hashes, type);

        if (missing.isEmpty()) {
            log.info("All {} blocks already exist, no upload needed", type);
            return;
        }

        log.info("Uploading {} missing {} blocks…", missing.size(), type);

        List<BlockDto> uploadList = new ArrayList<>(missing.size());
        for (String h : missing) {
            uploadList.add(new BlockDto(h, blockMap.get(h), type));
        }

        blockClient.uploadBlocksBatch(uploadList, type);
    }
}
