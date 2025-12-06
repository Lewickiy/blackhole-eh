package ru.levitsky.blackholeeh.service;

import lombok.extern.slf4j.Slf4j;
import ru.levitsky.blackholeeh.util.image.component.RctComponents;
import ru.levitsky.blackholeeh.util.image.RctTransformUtils;
import ru.levitsky.blackholeeh.util.image.component.RgbComponents;
import ru.levitsky.blackholeeh.util.image.RgbExtractorUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BlockSplitter {

    public record RctBlock(byte[] y, byte[] uPacked, byte[] vPacked) {
    }

    /**
     * Split image into padded 8x8 blocks using reversible integer transform (lossless).
     */
    public static List<RctBlock> splitIntoRctBlocks(File file) throws Exception {
        long startTime = System.nanoTime();
        BufferedImage image = javax.imageio.ImageIO.read(file);

        int width = image.getWidth();
        int height = image.getHeight();
        int paddedWidth = ((width + 7) / 8) * 8;
        int paddedHeight = ((height + 7) / 8) * 8;

        // padded image (fill extra pixels with edge pixels)
        BufferedImage padded = new BufferedImage(paddedWidth, paddedHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = padded.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        List<RctBlock> blocks = new ArrayList<>();

        for (int by = 0; by < paddedHeight; by += 8) {
            for (int bx = 0; bx < paddedWidth; bx += 8) {
                byte[] y = new byte[64];           // 8x8
                short[] uShorts = new short[64];   // 8x8
                short[] vShorts = new short[64];   // 8x8

                int idx = 0;
                for (int yoff = 0; yoff < 8; yoff++) {
                    for (int xoff = 0; xoff < 8; xoff++) {
                        RgbComponents rgb = RgbExtractorUtils.extractRgbFromPixel(padded, bx, by, xoff, yoff);
                        RctComponents rct = RctTransformUtils.forwardRctTransform(rgb.red(), rgb.green(), rgb.blue());

                        y[idx] = (byte) (rct.y() & 0xFF);
                        uShorts[idx] = (short) rct.u();
                        vShorts[idx] = (short) rct.v();
                        idx++;
                    }
                }

                byte[] uPacked = packShortArrayBE(uShorts);
                byte[] vPacked = packShortArrayBE(vShorts);

                blocks.add(new RctBlock(y, uPacked, vPacked));
            }
        }

        long timeMs = (System.nanoTime() - startTime) / 1_000_000;
        log.info("File '{}' split into {} RCT blocks in {} ms", file.getName(), blocks.size(), timeMs);
        return blocks;
    }

    private static byte[] packShortArrayBE(short[] arr) {
        ByteBuffer buf = ByteBuffer.allocate(arr.length * 2).order(ByteOrder.BIG_ENDIAN);
        for (short s : arr) buf.putShort(s);
        return buf.array();
    }
}
