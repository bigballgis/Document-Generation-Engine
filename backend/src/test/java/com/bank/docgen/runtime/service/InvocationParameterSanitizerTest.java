package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.ContextView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvocationParameterSanitizerTest {

    private InvocationParameterSanitizer sanitizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sanitizer = new InvocationParameterSanitizer(objectMapper);
    }

    @Test
    void sanitizeSingleRequest_stripsEncryptionPasswords() throws Exception {
        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("name", "Alice"),
                new EncryptionOptionsView(true, "secret-open", "secret-owner", List.of("PRINT")),
                "req-1",
                "idem-1",
                null
        );

        String json = sanitizer.sanitizeSingleRequest(request, "1.0.0");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("encryption").has("openPassword")).isFalse();
        assertThat(root.get("encryption").has("ownerPassword")).isFalse();
        assertThat(root.get("encryption").get("enabled").asBoolean()).isTrue();
        assertThat(root.get("encryption").get("openPasswordProvided").asBoolean()).isTrue();
        assertThat(root.get("encryption").get("ownerPasswordProvided").asBoolean()).isTrue();
        assertThat(root.has("variables")).isFalse();
        assertThat(root.get("variablesHash").asText()).hasSize(64);
        assertThat(root.has("contextSummary")).isFalse();
    }

    @Test
    void sanitizeSingleRequest_writesContextSummaryForNonBlankWhitelistKeys() throws Exception {
        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("ssn", "should-not-appear"),
                new EncryptionOptionsView(false, null, null, List.of()),
                "req-1",
                "idem-1",
                new ContextView("LOS", "API", "BR-1", "tr-1", "onboarding", "en-US")
        );

        String json = sanitizer.sanitizeSingleRequest(request, "1.0.0");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("contextSummary").get("sourceSystem").asText()).isEqualTo("LOS");
        assertThat(root.get("contextSummary").get("channel").asText()).isEqualTo("API");
        assertThat(root.get("contextSummary").get("locale").asText()).isEqualTo("en-US");
        assertThat(root.toString()).doesNotContain("should-not-appear");
        assertThat(root.toString()).doesNotContain("secret");
    }

    @Test
    void sanitizeSingleRequest_omitsBlankContextKeysFromSummary() throws Exception {
        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("name", "Alice"),
                null,
                "req-1",
                "idem-1",
                new ContextView("", "API", null, "  ", null, "en-US")
        );

        JsonNode root = objectMapper.readTree(sanitizer.sanitizeSingleRequest(request, "1.0.0"));

        assertThat(root.get("contextSummary").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("channel", "locale");
    }

    @Test
    void sanitizeBatchRequest_stripsPasswordsFromBatchAndItems() throws Exception {
        BatchGenerateRequestBody request = new BatchGenerateRequestBody(
                new OutputOptionsView("PDF", "SYNC_STREAM"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody(
                        "item-1",
                        Map.of("amount", 100),
                        null,
                        new EncryptionOptionsView(true, "item-open", "item-owner", List.of())
                )),
                new EncryptionOptionsView(true, "batch-open", "batch-owner", List.of("COPY")),
                "req-batch",
                "idem-batch",
                null,
                new ContextView(null, "BATCH", null, null, null, null)
        );

        String json = sanitizer.sanitizeBatchRequest(request, "2.0.0");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("encryption").has("openPassword")).isFalse();
        assertThat(root.get("items").get(0).get("encryption").has("ownerPassword")).isFalse();
        assertThat(root.get("items").get(0).get("encryption").get("ownerPasswordProvided").asBoolean()).isTrue();
        assertThat(root.toString()).doesNotContain("100");
        assertThat(root.get("itemsHash").asText()).hasSize(64);
        assertThat(root.get("items").get(0).has("variables")).isFalse();
        assertThat(root.get("items").get(0).get("variablesHash").asText()).hasSize(64);
        assertThat(root.get("contextSummary").get("channel").asText()).isEqualTo("BATCH");
        assertThat(root.has("originalBatchId")).isFalse();
    }

    @Test
    void bddCeC05_004_sanitizeBatchRequestPersistsOriginalBatchIdAssociation() throws Exception {
        BatchGenerateRequestBody request = new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody(
                        "item-retry",
                        Map.of("secretVar", "must-not-appear"),
                        null,
                        null
                )),
                null,
                "req-retry",
                "idem-retry",
                "BATCH-ORIG01",
                null
        );

        JsonNode root = objectMapper.readTree(sanitizer.sanitizeBatchRequest(request, "1.0.0"));

        assertThat(root.get("originalBatchId").asText()).isEqualTo("BATCH-ORIG01");
        assertThat(root.toString()).doesNotContain("must-not-appear");
        assertThat(root.toString()).doesNotContain("secretVar");
    }

    @Test
    void stripEncryptionPasswordsRewritesLegacyPayload() throws Exception {
        String raw = """
                {
                  "encryption": {
                    "enabled": true,
                    "openPassword": "pw",
                    "ownerPassword": "pw2",
                    "permissions": ["PRINT"]
                  }
                }
                """;

        String sanitized = sanitizer.stripEncryptionPasswords(raw);
        JsonNode root = objectMapper.readTree(sanitized);

        assertThat(root.get("encryption").has("openPassword")).isFalse();
        assertThat(root.get("encryption").get("openPasswordProvided").asBoolean()).isTrue();
    }
}
