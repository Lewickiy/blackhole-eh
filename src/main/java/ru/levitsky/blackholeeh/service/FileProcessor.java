package ru.levitsky.blackholeeh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.levitsky.blackholeeh.util.HashUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
                        e.printStackTrace();
                    }
                });
    }

    private void processFile(Path file) throws Exception {
        log.info("Processing file: {}", file.getFileName());
        List<byte[]> blocks = BlockSplitter.splitIntoBlocks8x8(file.toFile());

        for (byte[] block : blocks) {
            String hash = HashUtils.sha256(block);
            if (blockClient.getBlockByHash(hash).isEmpty()) {
                blockClient.uploadBlock(hash, block);
                log.info("Uploaded block: {}", hash);
            } else {
                log.info("Block already exists: {}", hash);
            }
        }
    }
}
