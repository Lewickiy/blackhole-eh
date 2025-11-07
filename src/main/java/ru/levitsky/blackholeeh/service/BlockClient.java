package ru.levitsky.blackholeeh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.levitsky.blackholeeh.dto.BlockResponse;
import ru.levitsky.blackholeeh.dto.BlockSaveRequest;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlockClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public Optional<BlockResponse> getBlockByHash(String hash) {
        try {
            ResponseEntity<BlockResponse> response =
                    restTemplate.getForEntity(baseUrl + "/api/v1/blocks/" + hash, BlockResponse.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public BlockResponse uploadBlock(String hash, byte[] data) {
        BlockSaveRequest request = new BlockSaveRequest(hash, data);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BlockSaveRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BlockResponse> response =
                restTemplate.postForEntity(baseUrl + "/api/v1/blocks", entity, BlockResponse.class);
        return response.getBody();
    }
}
