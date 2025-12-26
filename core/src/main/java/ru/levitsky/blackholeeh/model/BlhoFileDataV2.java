package ru.levitsky.blackholeeh.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Internal data structure representing the contents of a BLHO v2 file.<br>
 * Stores metadata, unique block hashes, and position maps for each Y/U/V component.<br>
 */
@Getter
@Setter
@AllArgsConstructor
public class BlhoFileDataV2 {
    /**
     * name of the source image file
     */
    String originalFileName;
    /**
     * width of the original image in pixels
     */
    int width;
    /**
     * height of the original image in pixels
     */
    int height;

    /**
     * list of SHA-256 hashes for unique Y (luminance) blocks
     */
    List<byte[]> uniqueYHashes;

    /**
     * list of SHA-256 hashes for unique U (chrominance) blocks
     */
    List<byte[]> uniqueUHashes;

    /**
     * list of SHA-256 hashes for unique V (chrominance) blocks
     */
    List<byte[]> uniqueVHashes;
    /**
     * map of Y block indices to reconstruct the original layout
     */
    List<Integer> yPositionMap;
    /**
     * map of U block indices to reconstruct the original layout
     */
    List<Integer> uPositionMap;
    /**
     * map of V block indices to reconstruct the original layout
     */
    List<Integer> vPositionMap;

    /**
     * @return the total number of blocks in the image
     */
    public int totalBlocks() {
        return yPositionMap.size();
    }
}
