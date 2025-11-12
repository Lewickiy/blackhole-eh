package ru.levitsky.blackholeeh.service;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BlockSplitter {

    public static List<byte[]> splitIntoBlocks8x8(File file) throws Exception {
        long startTotal = System.nanoTime();

        BufferedImage image = ImageIO.read(file);
        List<byte[]> blocks = new ArrayList<>();

        int width = image.getWidth();
        int height = image.getHeight();
        int blockCount = 0;

        long blockTimeTotal = 0;

        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x += 8) {
                long startBlock = System.nanoTime();

                int blockWidth = Math.min(8, width - x);
                int blockHeight = Math.min(8, height - y);

                BufferedImage block = image.getSubimage(x, y, blockWidth, blockHeight);
                byte[] blockBytes = getBytesFromBlock(block);
                blocks.add(blockBytes);

                blockTimeTotal += System.nanoTime() - startBlock;
                blockCount++;
            }
        }

        long totalTime = System.nanoTime() - startTotal;
        double totalMs = totalTime / 1_000_000.0;
        double avgBlockMs = (blockCount > 0)
                ? (blockTimeTotal / 1_000_000.0 / blockCount)
                : 0;

        log.info("Split '{}' into {} blocks in {} ms (avg {} ms per block)",
                file.getName(), blockCount, totalMs, avgBlockMs);

        return blocks;
    }

    private static byte[] getBytesFromBlock(BufferedImage block) {
        int w = block.getWidth();
        int h = block.getHeight();
        byte[] bytes = new byte[w * h * 3]; //RGB
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = block.getRGB(x, y);
                bytes[idx++] = (byte) ((rgb >> 16) & 0xFF);
                bytes[idx++] = (byte) ((rgb >> 8) & 0xFF);
                bytes[idx++] = (byte) (rgb & 0xFF);
            }
        }
        return bytes;
    }
}
