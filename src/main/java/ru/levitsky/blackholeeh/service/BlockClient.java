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
     * Checks which blocks from the provided list are missing on the server.
     *
     * <p>This method sends a batch request to the server to verify the existence of blocks
     * identified by their SHA-256 hashes. The server responds with a list of hashes that
     * are not present in its storage.
     *
     * <p><b>Flow:</b>
     * <ol>
     *   <li>Validates input parameters</li>
     *   <li>Constructs REST API URL with block type parameter</li>
     *   <li>Sends POST request with block hashes to server</li>
     *   <li>Processes server response to extract missing blocks</li>
     *   <li>Logs operation results for monitoring</li>
     * </ol>
     *
     * <p><b>Error Handling:</b>
     * <ul>
     *   <li>Returns empty list if input hashes list is empty</li>
     *   <li>Returns empty list if network error or server unavailable</li>
     *   <li>Logs detailed error information for troubleshooting</li>
     * </ul>
     *
     * <p><b>Example Usage:</b>
     * <pre>
     * {@code
     * List<String> hashes = Arrays.asList("abc123...", "def456...");
     * List<String> missing = blockClient.checkMissingBlocks(hashes, BlockType.LUMA);
     * // missing will contain only hashes not found on server
     * }
     * </pre>
     *
     * @param hashes the list of block hashes to check for existence on server.
     *               Each hash should be SHA-256 hexadecimal string.
     *               Must not be null, but can be empty.
     * @param type   the type of blocks being checked (LUMA, CHROMA_CB, CHROMA_CR).
     *               Determines which storage partition to query on server.
     * @return a list of hashes that are missing on the server. Returns empty list if:
     * - input hashes list is empty
     * - all blocks exist on server
     * - network error occurs
     * - server returns error response
     * @throws NullPointerException if hashes list is null (though method handles gracefully in practice)
     * @see BlockCheckRequest
     * @see BlockCheckResponse
     * @see BlockType
     * @see #uploadBlocksBatch(List, BlockType)
     */
    public List<String> checkMissingBlocks(List<String> hashes, BlockType type) {
        // Early return for empty input - no need to make network call
        if (hashes.isEmpty()) {
            return Collections.emptyList();
        }
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

        List<List<BlockDto>> batches = partition(blocks);
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

    private static <T> List<List<T>> partition(List<T> list) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            parts.add(list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
        }
        return parts;
    }
}
