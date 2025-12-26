package ru.levitsky.blackholeeh.util.image;

import ru.levitsky.blackholeeh.util.image.component.RctComponents;

/**
 * Utility methods for Reversible Color Transform (RCT) operations.
 * RCT provides lossless color space conversion between RGB and YUV.
 */
public final class RctTransformUtils {

    public static RctComponents forwardRctTransform(int r, int g, int b) {
        int Y = (calculateLuma(r, g, b));
        int[] chroma = calculateChroma(r, g, b);
        int U = chroma[0];
        int V = chroma[1];

        return new RctComponents(Y, U, V);
    }

    private RctTransformUtils() {
    }

    /**
     * Calculates only the luma (Y) component from RGB.
     * Useful when only brightness information is needed.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return luma component Y (0-255)
     */
    public static int calculateLuma(int r, int g, int b) {
        return (r + 2 * g + b) >> 2;
    }

    /**
     * Calculates chroma components (U and V) from RGB.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return chroma components as [U, V] array
     */
    public static int[] calculateChroma(int r, int g, int b) {
        return new int[]{r - g, b - g};
    }
}
