package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
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
                "idem-1"
        );

        String json = sanitizer.sanitizeSingleRequest(request, "1.0.0");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("encryption").has("openPassword")).isFalse();
        assertThat(root.get("encryption").has("ownerPassword")).isFalse();
        assertThat(root.get("encryption").get("enabled").asBoolean()).isTrue();
        assertThat(root.get("encryption").get("openPasswordProvided").asBoolean()).isTrue();
        assertThat(root.get("encryption").get("ownerPasswordProvided").asBoolean()).isTrue();
        assertThat(root.get("variables").get("name").asText()).isEqualTo("Alice");
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
                "idem-batch"
        );

        String json = sanitizer.sanitizeBatchRequest(request, "2.0.0");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("encryption").has("openPassword")).isFalse();
        assertThat(root.get("items").get(0).get("encryption").has("ownerPassword")).isFalse();
        assertThat(root.get("items").get(0).get("encryption").get("ownerPasswordProvided").asBoolean()).isTrue();
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
