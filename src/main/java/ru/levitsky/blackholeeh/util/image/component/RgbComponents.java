package ru.levitsky.blackholeeh.util.image.component;

/**
 * Immutable record representing RGB color components without alpha channel.
 * Optimized for JPEG images and formats that don't support transparency.
 *
 * <p><b>Usage:</b> For JPEG processing, RCT transformations, and opaque images.
 *
 * @param red   red component (0-255)
 * @param green green component (0-255)
 * @param blue  blue component (0-255)
 */
public record RgbComponents(int red, int green, int blue) {

    /**
     * Canonical constructor with validation.
     *
     * @param red   red component (0-255)
     * @param green green component (0-255)
     * @param blue  blue component (0-255)
     * @throws IllegalArgumentException if any component is outside valid range
     */
    public RgbComponents {
        if (red < 0 || red > 255) {
            throw new IllegalArgumentException(String.format("Red component must be in range 0-255: %s", red));
        }
        if (green < 0 || green > 255) {
            throw new IllegalArgumentException("Green component must be in range 0-255: " + green);
        }
        if (blue < 0 || blue > 255) {
            throw new IllegalArgumentException("Blue component must be in range 0-255: " + blue);
        }
    }
}
