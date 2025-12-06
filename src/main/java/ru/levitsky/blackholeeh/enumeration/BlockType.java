package ru.levitsky.blackholeeh.enumeration;

/**
 * Enum representing the type of block in the Blackhole image processing system.
 * <p>
 * Each block type corresponds to a component in the Reversible Color Transform (RCT):
 * <ul>
 *     <li>{@link #LUMA} – the luminance (Y) component of the image.</li>
 *     <li>{@link #CHROMA_CB} – the blue-difference chroma (Cb) component.</li>
 *     <li>{@link #CHROMA_CR} – the red-difference chroma (Cr) component.</li>
 * </ul>
 * These types are used for deduplication, hashing, storage, and uploading blocks
 * to the server.
 */
public enum BlockType {
    /**
     * Luminance component (Y)
     */
    LUMA,

    /**
     * Blue-difference chroma component (Cb)
     */
    CHROMA_CB,

    /**
     * Red-difference chroma component (Cr)
     */
    CHROMA_CR
}
