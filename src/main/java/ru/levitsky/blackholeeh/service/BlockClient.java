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
    public List<String> checkMissingBlocks(List<String> hashes) {
        if (hashes.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            BlockCheckRequest req = new BlockCheckRequest(hashes);
            ResponseEntity<BlockCheckResponse> resp =
                    restTemplate.postForEntity(BASE_URL + "/check", req, BlockCheckResponse.class);

            List<String> missing = Objects.requireNonNull(resp.getBody()).getMissing();
            int existing = hashes.size() - missing.size();
            log.info("Checked {} blocks → {} exist, {} missing", hashes.size(), existing, missing.size());
            return missing;
        } catch (Exception e) {
            log.error("Error checking missing blocks: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Загружает блоки на сервер пакетами, чтобы не превышать лимит параметров
     */
    public void uploadBlocksBatch(List<BlockDto> blocks) {
        if (blocks.isEmpty()) {
            log.info("No blocks to upload");
            return;
        }

        List<List<BlockDto>> batches = partition(blocks, BATCH_SIZE);
        for (int i = 0; i < batches.size(); i++) {
            List<BlockDto> batch = batches.get(i);
            BlockBatchUploadRequest req = new BlockBatchUploadRequest(batch);

            try {
                restTemplate.postForEntity(BASE_URL + "/upload", req, Void.class);
                log.info("Uploaded batch {}/{} ({} blocks)",
                        i + 1, batches.size(), batch.size());
            } catch (Exception e) {
                log.error("Upload failed for batch {}/{}: {}", i + 1, batches.size(), e.getMessage());
            }
        }
    }

    /**
     * Делит список на подсписки фиксированного размера
     */
    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }
}
