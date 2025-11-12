package ru.levitsky.blackholeeh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockDto {
    private String hash;
    private byte[] data;
}
