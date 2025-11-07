package ru.levitsky.blackholeeh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockSaveRequest {
    private String hash;
    private byte[] data;
}
