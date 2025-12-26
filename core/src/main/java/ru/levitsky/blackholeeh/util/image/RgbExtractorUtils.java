package ru.levitsky.blackholeeh.util.image;

import ru.levitsky.blackholeeh.util.image.component.RgbComponents;

import java.awt.image.BufferedImage;

/**
 * Utility methods for extracting and manipulating RGB color components from images and integer values.
 * Provides type-safe operations for working with RGB color space.
 */
public final class RgbExtractorUtils {

    private RgbExtractorUtils() {
    }

    /**
     * Extracts RGB components from a pixel in the padded image using block coordinates and offsets.
     *
     * <p>Calculates absolute coordinates: {@code (bx + xoff, by + yoff)}
     *
     * <p><b>Coordinate System:</b>
     * <pre>
     * by ↗
     *    ┌────────────────────┐
     *    │ bx → • (xoff,yoff) │
     *    │                    │
     *    │    8×8 BLOCK       │
     *    └────────────────────┘
     * </pre>
     *
     * @param padded the padded image (BufferedImage.TYPE_INT_RGB)
     * @param bx     block X coordinate (must be multiple of 8)
     * @param by     block Y coordinate (must be multiple of 8)
     * @param xoff   X offset within the block (0-7 inclusive)
     * @param yoff   Y offset within the block (0-7 inclusive)
     * @return RgbComponents instance containing red, green, and blue values
     * @throws IllegalArgumentException       if coordinates or offsets are invalid
     * @throws ArrayIndexOutOfBoundsException if calculated coordinates exceed image boundaries
     */
    public static RgbComponents extractRgbFromPixel(BufferedImage padded, int bx, int by, int xoff, int yoff) {
        validateBlockCoordinates(bx, by);
        validateBlockOffsets(xoff, yoff);

        int absoluteX = bx + xoff;
        int absoluteY = by + yoff;
        validateImageCoordinates(padded, absoluteX, absoluteY);

        int rgb = padded.getRGB(absoluteX, absoluteY);
        int rVal = extractRed(rgb);
        int gVal = extractGreen(rgb);
        int bVal = extractBlue(rgb);

        return new RgbComponents(rVal, gVal, bVal);
    }

    /**
     * Extracts the red component from an integer RGB value.
     *
     * <p><b>Bit manipulation:</b>
     * <pre>
     * RGB format: 0xAARRGGBB
     * Red component: bits 16-23
     * Extraction: (rgb >> 16) & 0xFF
     * </pre>
     *
     * @param rgb the RGB value in 0xAARRGGBB format
     * @return red component in range 0-255
     */
    public static int extractRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    /**
     * Extracts the green component from an integer RGB value.
     *
     * <p><b>Bit manipulation:</b>
     * <pre>
     * RGB format: 0xAARRGGBB
     * Green component: bits 8-15
     * Extraction: (rgb >> 8) & 0xFF
     * </pre>
     *
     * @param rgb the RGB value in 0xAARRGGBB format
     * @return green component in range 0-255
     */
    public static int extractGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    /**
     * Extracts the blue component from an integer RGB value.
     *
     * <p><b>Bit manipulation:</b>
     * <pre>
     * RGB format: 0xAARRGGBB
     * Blue component: bits 0-7
     * Extraction: rgb & 0xFF
     * </pre>
     *
     * @param rgb the RGB value in 0xAARRGGBB format
     * @return blue component in range 0-255
     */
    public static int extractBlue(int rgb) {
        return rgb & 0xFF;
    }

    private static void validateBlockCoordinates(int bx, int by) {
        if (bx % 8 != 0 || by % 8 != 0) {
            throw new IllegalArgumentException(
                    String.format("Block coordinates must be multiples of 8: bx=%d, by=%d", bx, by));
        }
    }

    private static void validateBlockOffsets(int xoff, int yoff) {
        if (xoff < 0 || xoff >= 8 || yoff < 0 || yoff >= 8) {
            throw new IllegalArgumentException(
                    String.format("Offsets must be in range 0-7: xoff=%d, yoff=%d", xoff, yoff));
        }
    }

    private static void validateImageCoordinates(BufferedImage image, int x, int y) {
        if (x >= image.getWidth() || y >= image.getHeight() || x < 0 || y < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    String.format("Coordinates exceed image boundaries: (%d, %d). Image dimensions: %dx%d",
                            x, y, image.getWidth(), image.getHeight()));
        }
    }
}