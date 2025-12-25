package ru.levitsky.blackholeeh.util.image.component;

/**
 * Immutable record representing RCT color components.
 */
public record RctComponents(int y, int u, int v) {
    /**
     * Creates RCT components with validation.
     */
    public RctComponents {
        if (y < 0 || y > 255) {
            throw new IllegalArgumentException("Y component must be 0-255: " + y);
        }
    }
}