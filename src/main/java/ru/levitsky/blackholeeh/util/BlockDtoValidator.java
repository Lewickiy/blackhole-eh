package ru.levitsky.blackholeeh.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import ru.levitsky.blackholeeh.dto.BlockDto;

import java.util.Set;

public class BlockDtoValidator {
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

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
