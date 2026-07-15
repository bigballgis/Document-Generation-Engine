package com.bank.docgen.runtime.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-C05-003 / 010 — BatchResult echo field + OpenAPI ErrorCode alignment.
 */
class OriginalBatchIdContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bddCeC05_003_batchResultSerializesOriginalBatchIdWhenPresent() throws Exception {
        BatchResultView withLineage = new BatchResultView(
                "BATCH-NEW01",
                new BatchSummaryView(1, 1, 1, 0, 0),
                List.of(),
                "BATCH-ORIG02"
        );

        String json = objectMapper.writeValueAsString(withLineage);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("batchId").asText()).isEqualTo("BATCH-NEW01");
        assertThat(root.get("originalBatchId").asText()).isEqualTo("BATCH-ORIG02");
        assertThat(root.get("batchId").asText()).isNotEqualTo(root.get("originalBatchId").asText());
    }

    @Test
    void bddCeC05_001_batchResultOmitsNullOriginalBatchId() throws Exception {
        BatchResultView plain = new BatchResultView(
                "BATCH-NEW02",
                new BatchSummaryView(1, 1, 1, 0, 0),
                List.of()
        );

        String json = objectMapper.writeValueAsString(plain);
        assertThat(json).doesNotContain("originalBatchId");
    }

    @Test
    void bddCeC05_010_openapiDeclaresOriginalBatchNotFoundAndBatchResultField() throws Exception {
        Path openApi = Path.of("docs/api/openapi-v1.yaml");
        if (!Files.isRegularFile(openApi)) {
            openApi = Path.of("../docs/api/openapi-v1.yaml");
        }
        assertThat(openApi).exists();

        String yaml = Files.readString(openApi);
        assertThat(yaml).contains("ORIGINAL_BATCH_NOT_FOUND");
        assertThat(yaml).contains("originalBatchId");

        int errorCodeIdx = yaml.indexOf("ErrorCode:");
        assertThat(errorCodeIdx).isGreaterThanOrEqualTo(0);
        Matcher matcher = Pattern.compile("^\\s+- ORIGINAL_BATCH_NOT_FOUND$", Pattern.MULTILINE)
                .matcher(yaml.substring(errorCodeIdx));
        assertThat(matcher.find()).isTrue();

        int batchResultIdx = yaml.indexOf("BatchResult:");
        assertThat(batchResultIdx).isGreaterThanOrEqualTo(0);
        assertThat(yaml.substring(batchResultIdx, batchResultIdx + 800)).contains("originalBatchId:");
    }
}
