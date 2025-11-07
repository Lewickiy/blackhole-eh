package ru.levitsky.blackholeeh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.levitsky.blackholeeh.util.HashUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileProcessor {
    private final BlockClient blockClient;

    public void processDirectory(String dirPath) throws IOException {
        Path dir = Path.of(dirPath);
        if (!Files.isDirectory(dir)) {
            System.err.println("❌ " + dir + " is not a directory!");
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
        System.out.println("🖼 Processing file: " + file.getFileName());
        List<byte[]> blocks = BlockSplitter.splitIntoBlocks8x8(file.toFile());

        for (byte[] block : blocks) {
            String hash = HashUtils.sha256(block);
            if (blockClient.getBlockByHash(hash).isEmpty()) {
                blockClient.uploadBlock(hash, block);
                System.out.println("⬆️ Uploaded block " + hash);
            } else {
                System.out.println("⏩ Block already exists " + hash);
            }
        }
    }
}
