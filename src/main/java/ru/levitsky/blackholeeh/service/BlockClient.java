package ru.levitsky.blackholeeh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.levitsky.blackholeeh.dto.BlockBatchUploadRequest;
import ru.levitsky.blackholeeh.dto.BlockCheckRequest;
import ru.levitsky.blackholeeh.dto.BlockCheckResponse;
import ru.levitsky.blackholeeh.dto.BlockDto;
import ru.levitsky.blackholeeh.enumeration.BlockType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockClient {
    private final RestTemplate restTemplate;

    private static final String BASE_URL = "http://localhost:8081/api/v1/blocks";
    private static final int BATCH_SIZE = 1000;

    /**
     * Проверяет, какие блоки отсутствуют на сервере
     */
    public List<String> checkMissingBlocks(List<String> hashes, BlockType type) {
        if (hashes.isEmpty()) return Collections.emptyList();
        try {
            String url = BASE_URL + "/check?type=" + type.name();
            BlockCheckRequest req = new BlockCheckRequest(hashes);
            ResponseEntity<BlockCheckResponse> resp =
                    restTemplate.postForEntity(url, req, BlockCheckResponse.class);

            List<String> missing = Objects.requireNonNull(resp.getBody()).getMissing();
            log.info("Checked {} {} blocks → {} missing", hashes.size(), type, missing.size());
            return missing;
        } catch (Exception e) {
            log.error("Error checking missing {} blocks: {}", type, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Загружает блоки на сервер пакетами
     */
    public void uploadBlocksBatch(List<BlockDto> blocks, BlockType type) {
        if (blocks.isEmpty()) return;

        List<List<BlockDto>> batches = partition(blocks, BATCH_SIZE);
        for (int i = 0; i < batches.size(); i++) {
            List<BlockDto> batch = batches.get(i);
            String url = BASE_URL + "/upload?type=" + type.name();
            BlockBatchUploadRequest req = new BlockBatchUploadRequest(batch);

            try {
                restTemplate.postForEntity(url, req, Void.class);
                log.info("Uploaded batch {}/{} of {} blocks", i + 1, batches.size(), type);
            } catch (Exception e) {
                log.error("Upload failed for batch {}/{}: {}", i + 1, batches.size(), e.getMessage());
            }
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }
}
