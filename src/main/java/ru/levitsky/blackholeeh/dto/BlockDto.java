package ru.levitsky.blackholeeh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.levitsky.blackholeeh.enumeration.BlockType;

/**
 * DTO representing a single block of an image for Blackhole processing.
 * <p>
 * Each block consists of:
 * <ul>
 *     <li>{@code hash}: SHA-256 hash (or similar) of the block's content, used for deduplication.</li>
 *     <li>{@code data}: Raw bytes of the block (Y, U, or V channel packed data).</li>
 *     <li>{@code type}: Type of the block, indicating which channel it represents (LUMA, CHROMA_CB, CHROMA_CR).</li>
 * </ul>
 * <p>
 * Validation annotations ensure:
 * <ul>
 *     <li>{@code hash} is not blank and matches a hexadecimal string pattern.</li>
 *     <li>{@code data} is non-null and non-empty.</li>
 *     <li>{@code type} is not null.</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockDto {

    /**
     * SHA-256 (or similar) hash of the block data.
     * Must be a hexadecimal string between 16 and 128 characters.
     */
    @NotBlank(message = "Hash must not be blank")
    @Pattern(regexp = "^[a-fA-F0-9]{16,128}$", message = "Hash must be hex (16-128 chars)")
    private String hash;

    /**
     * Raw byte array of the block content.
     * Cannot be null or empty.
     */
    @NotNull(message = "Data must not be null")
    @Size(min = 1, message = "Data must not be empty")
    private byte[] data;

    /**
     * Type of the block ({@link BlockType#LUMA}, {@link BlockType#CHROMA_CB}, {@link BlockType#CHROMA_CR}).
     */
    @NotNull(message = "Block type must not be null")
    private BlockType type;
}
