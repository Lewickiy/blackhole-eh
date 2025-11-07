package ru.levitsky.blackholeeh.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BlockSplitter {

    public static List<byte[]> splitIntoBlocks8x8(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        List<byte[]> blocks = new ArrayList<>();

        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x += 8) {
                int blockWidth = Math.min(8, width - x);
                int blockHeight = Math.min(8, height - y);

                BufferedImage block = image.getSubimage(x, y, blockWidth, blockHeight);
                byte[] blockBytes = getBytesFromBlock(block);
                blocks.add(blockBytes);
            }
        }
        return blocks;
    }

    private static byte[] getBytesFromBlock(BufferedImage block) {
        int w = block.getWidth();
        int h = block.getHeight();
        byte[] bytes = new byte[w * h * 3]; // RGB
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = block.getRGB(x, y);
                bytes[idx++] = (byte) ((rgb >> 16) & 0xFF); // R
                bytes[idx++] = (byte) ((rgb >> 8) & 0xFF);  // G
                bytes[idx++] = (byte) (rgb & 0xFF);         // B
            }
        }
        return bytes;
    }
}
