package ru.levitsky.blackholeeh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.levitsky.blackholeeh.dto.BlockDto;
import ru.levitsky.blackholeeh.util.HashUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessor {

    private final BlockClient blockClient;

    public void processDirectory(String dirPath) throws IOException {
        Path dir = Path.of(dirPath);
        if (!Files.isDirectory(dir)) {
            log.error("'{}' is not a directory", dir);
            return;
        }

        Files.list(dir)
                .filter(p -> p.toString().toLowerCase().endsWith(".jpg"))
                .forEach(p -> {
                    try {
                        processFile(p);
                    } catch (Exception e) {
                        log.error("Error processing {}: {}", p, e.getMessage());
                    }
                });
    }

    private void processFile(Path file) throws Exception {
        log.info("Processing file: {}", file.getFileName());
        List<byte[]> blocks = BlockSplitter.splitIntoBlocks8x8(file.toFile());

        Map<String, byte[]> blockMap = new LinkedHashMap<>();
        for (byte[] block : blocks) {
            blockMap.put(HashUtils.sha256(block), block);
        }

        List<String> hashes = new ArrayList<>(blockMap.keySet());
        List<String> missing = blockClient.checkMissingBlocks(hashes);

        log.info("Checked {} blocks: {} missing, {} already exist", hashes.size(), missing.size(), hashes.size() - missing.size());

        if (missing.isEmpty()) {
            log.info("File {} skipped - all blocks already exist", file.getFileName());
            return;
        }

        // формируем список на загрузку
        List<BlockDto> toUpload = missing.stream()
                .map(h -> new BlockDto(h, blockMap.get(h)))
                .toList();

        // 🔥 ограничиваем размер батча
        final int batchSize = 1000; // можно вынести в application.properties
        for (int i = 0; i < toUpload.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toUpload.size());
            List<BlockDto> batch = toUpload.subList(i, end);

            blockClient.uploadBlocksBatch(batch);
            log.info("Uploaded batch {}/{} ({} blocks)",
                    (i / batchSize) + 1,
                    (int) Math.ceil((double) toUpload.size() / batchSize),
                    batch.size());
        }
        log.info("File {} processed completely: uploaded {} new blocks", file.getFileName(), missing.size());
    }
}
