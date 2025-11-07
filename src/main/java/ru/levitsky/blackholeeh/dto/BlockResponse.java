package ru.levitsky.blackholeeh.dto;

import lombok.Data;

@Data
public class BlockResponse {
    private String hash;
    private int size;
    private byte[] data;
}
