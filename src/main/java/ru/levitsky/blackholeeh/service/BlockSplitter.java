package ru.levitsky.blackholeeh.service;

import lombok.extern.slf4j.Slf4j;

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
                byte[] y = new byte[64];            // 8x8
                short[] uShorts = new short[64];   // 8x8
                short[] vShorts = new short[64];   // 8x8

                int idx = 0;
                for (int yoff = 0; yoff < 8; yoff++) {
                    for (int xoff = 0; xoff < 8; xoff++) {
                        int rgb = padded.getRGB(bx + xoff, by + yoff);
                        int r = (rgb >> 16) & 0xFF;
                        int gVal = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        int Y = (r + 2 * gVal + b) >> 2;
                        int U = r - gVal;
                        int V = b - gVal;

                        y[idx] = (byte) (Y & 0xFF);
                        uShorts[idx] = (short) U;
                        vShorts[idx] = (short) V;
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

    public static byte[] reconstructRgb(RctBlock block) {
        byte[] y = block.y();
        short[] u = unpackShortArrayBE(block.uPacked());
        short[] v = unpackShortArrayBE(block.vPacked());

        byte[] rgb = new byte[64 * 3];
        for (int i = 0, ri = 0; i < 64; i++) {
            int Y = y[i] & 0xFF;
            int U = u[i];
            int V = v[i];

            int G = Y - ((U + V) >> 2);
            int R = U + G;
            int B = V + G;

            rgb[ri++] = (byte) (R & 0xFF);
            rgb[ri++] = (byte) (G & 0xFF);
            rgb[ri++] = (byte) (B & 0xFF);
        }
        return rgb;
    }

    private static byte[] packShortArrayBE(short[] arr) {
        ByteBuffer buf = ByteBuffer.allocate(arr.length * 2).order(ByteOrder.BIG_ENDIAN);
        for (short s : arr) buf.putShort(s);
        return buf.array();
    }

    private static short[] unpackShortArrayBE(byte[] packed) {
        short[] arr = new short[packed.length / 2];
        ByteBuffer buf = ByteBuffer.wrap(packed).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < arr.length; i++) arr[i] = buf.getShort();
        return arr;
    }
}
