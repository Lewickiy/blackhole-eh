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
 * Основное дто для единичного объекта блока
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockDto {

    @NotBlank(message = "Hash must not be blank")
    @Pattern(regexp = "^[a-fA-F0-9]{16,128}$", message = "Hash must be hex (16-128 chars)")
    private String hash;

    @NotNull(message = "Data must not be null")
    @Size(min = 1, message = "Data must not be empty")
    private byte[] data;

    @NotNull(message = "Block type must not be null")
    private BlockType type;
}
