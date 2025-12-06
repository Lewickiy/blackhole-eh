package ru.levitsky.blackholeeh.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import ru.levitsky.blackholeeh.dto.BlockDto;

import java.util.Set;

/**
 * Utility class for validating {@link BlockDto} instances using Jakarta Bean Validation.
 * <p>
 * Provides a static method {@link #validate(BlockDto)} to check a block for constraint violations
 * and throw an exception if any are found.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * BlockDto block = new BlockDto("hole1234", data, BlockType.LUMA);
 * BlockDtoValidator.validate(block); // throws IllegalArgumentException if invalid
 * }
 * </pre>
 */
public class BlockDtoValidator {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    /**
     * Validates the given {@link BlockDto}.
     * <p>
     * If the DTO violates any constraints (e.g., blank hash, null data, null type),
     * this method throws an {@link IllegalArgumentException} with a detailed message.
     *
     * @param block the {@link BlockDto} to validate
     * @throws IllegalArgumentException if the block is invalid
     */
    public static void validate(BlockDto block) {
        Set<ConstraintViolation<BlockDto>> violations = validator.validate(block);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<BlockDto> v : violations) {
                sb.append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("\n");
            }
            throw new IllegalArgumentException("BlockDto validation failed:\n" + sb);
        }
    }
}
