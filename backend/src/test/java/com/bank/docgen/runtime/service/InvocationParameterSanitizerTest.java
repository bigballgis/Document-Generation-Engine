package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.ContextView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.security.VariableHashSupport;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvocationParameterSanitizerTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;

    private InvocationParameterSanitizer sanitizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sanitizer = new InvocationParameterSanitizer(
                objectMapper,
                templateVersionRepository,
                variableSchemaRepository
        );
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

        String json = sanitizer.sanitizeSingleRequest(
                request,
                "1.0.0",
                Map.of("name", VariablePiiCategory.NONE)
        );
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("encryption").has("openPassword")).isFalse();
        assertThat(root.get("encryption").has("ownerPassword")).isFalse();
        assertThat(root.get("encryption").get("enabled").asBoolean()).isTrue();
        assertThat(root.get("encryption").get("openPasswordProvided").asBoolean()).isTrue();
        assertThat(root.get("encryption").get("ownerPasswordProvided").asBoolean()).isTrue();
        assertThat(root.get("variables").get("name").asText()).isEqualTo("Alice");
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

        String json = sanitizer.sanitizeSingleRequest(
                request,
                "1.0.0",
                Map.of("ssn", VariablePiiCategory.NONE)
        );
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("contextSummary").get("sourceSystem").asText()).isEqualTo("LOS");
        assertThat(root.get("contextSummary").get("channel").asText()).isEqualTo("API");
        assertThat(root.get("contextSummary").get("locale").asText()).isEqualTo("en-US");
        assertThat(root.get("variables").get("ssn").asText()).isEqualTo("should-not-appear");
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

        JsonNode root = objectMapper.readTree(sanitizer.sanitizeSingleRequest(
                request,
                "1.0.0",
                Map.of("name", VariablePiiCategory.NONE)
        ));

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

        String json = sanitizer.sanitizeBatchRequest(
                request,
                "2.0.0",
                Map.of("amount", VariablePiiCategory.NONE)
        );
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("encryption").has("openPassword")).isFalse();
        assertThat(root.get("items").get(0).get("encryption").has("ownerPassword")).isFalse();
        assertThat(root.get("items").get(0).get("encryption").get("ownerPasswordProvided").asBoolean()).isTrue();
        // BATCH_ROOT item summaries stay hash-only; top-level variables present for single-item batches.
        assertThat(root.get("items").get(0).has("variables")).isFalse();
        assertThat(root.get("items").get(0).get("variablesHash").asText()).hasSize(64);
        assertThat(root.get("variables").get("amount").asInt()).isEqualTo(100);
        assertThat(root.get("itemsHash").asText()).hasSize(64);
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

        JsonNode root = objectMapper.readTree(sanitizer.sanitizeBatchRequest(
                request,
                "1.0.0",
                Map.of("secretVar", VariablePiiCategory.NONE)
        ));

        assertThat(root.get("originalBatchId").asText()).isEqualTo("BATCH-ORIG01");
        assertThat(root.get("variables").get("secretVar").asText()).isEqualTo("must-not-appear");
        assertThat(root.get("items").get(0).has("variables")).isFalse();
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

    @Test
    void bddIblA5_001_sanitizeSingleRequestRedactsPiiAndRetainsNone() throws Exception {
        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("customerName", "Alice Example", "productCode", "PRD-1"),
                null,
                "req-pii",
                "idem-pii",
                null
        );

        JsonNode root = objectMapper.readTree(sanitizer.sanitizeSingleRequest(
                request,
                "1.0.0",
                Map.of(
                        "customerName", VariablePiiCategory.PERSONAL_NAME,
                        "productCode", VariablePiiCategory.NONE
                )
        ));

        assertThat(root.toString()).doesNotContain("Alice Example");
        assertThat(root.get("variables").get("productCode").asText()).isEqualTo("PRD-1");
        assertThat(root.get("variables").has("customerName")).isFalse();
        assertThat(root.get("redactedVariableKeys").get(0).asText()).isEqualTo("customerName");
        assertThat(root.get("variablesHash").asText()).isEqualTo(
                VariableHashSupport.hashVariables(
                        objectMapper,
                        Map.of("customerName", "Alice Example", "productCode", "PRD-1")
                )
        );
    }

    @Test
    void bddIblA5_007_sanitizeBatchItemRedactsPiiProbe() throws Exception {
        BatchGenerateRequestBody request = new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody(
                        "item-1",
                        Map.of("customerName", "batch-pii-probe", "productCode", "BATCH-OK"),
                        null,
                        null
                )),
                null,
                "req-batch-pii",
                "idem-batch-pii",
                null,
                null
        );

        JsonNode root = objectMapper.readTree(sanitizer.sanitizeBatchItem(
                request.items().get(0),
                request,
                "1.0.0",
                Map.of(
                        "customerName", VariablePiiCategory.CONTACT,
                        "productCode", VariablePiiCategory.NONE
                )
        ));

        assertThat(root.toString()).doesNotContain("batch-pii-probe");
        assertThat(root.get("variables").get("productCode").asText()).isEqualTo("BATCH-OK");
        assertThat(root.get("redactedVariableKeys").get(0).asText()).isEqualTo("customerName");
    }

    @Test
    void bddIblA5_010_passwordStripAndPiiRedactionTogether() throws Exception {
        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("PDF", "SYNC_STREAM"),
                Map.of("customerName", "Alice Example", "productCode", "PRD-1"),
                new EncryptionOptionsView(true, "secret-open", "secret-owner", List.of("PRINT")),
                "req-both",
                "idem-both",
                null
        );

        JsonNode root = objectMapper.readTree(sanitizer.sanitizeSingleRequest(
                request,
                "1.0.0",
                Map.of(
                        "customerName", VariablePiiCategory.PERSONAL_NAME,
                        "productCode", VariablePiiCategory.NONE
                )
        ));

        assertThat(root.toString()).doesNotContain("Alice Example");
        assertThat(root.toString()).doesNotContain("secret-open");
        assertThat(root.toString()).doesNotContain("secret-owner");
        assertThat(root.get("variables").get("productCode").asText()).isEqualTo("PRD-1");
        assertThat(root.get("encryption").get("openPasswordProvided").asBoolean()).isTrue();
    }

    @Test
    void sanitizeSingleRequest_loadsPiiCategoriesFromTemplateVersionSchema() throws Exception {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "U0000001");
        version.setReleaseVersion("1.0.0");
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(
                        schema("customerName", VariablePiiCategory.PERSONAL_NAME),
                        schema("productCode", VariablePiiCategory.NONE)
                ));

        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("customerName", "Alice Example", "productCode", "PRD-1"),
                null,
                "req-schema",
                "idem-schema",
                null
        );

        JsonNode root = objectMapper.readTree(
                sanitizer.sanitizeSingleRequest(request, "1.0.0", TEMPLATE_ID)
        );

        assertThat(root.toString()).doesNotContain("Alice Example");
        assertThat(root.get("variables").get("productCode").asText()).isEqualTo("PRD-1");
    }

    @Test
    void sanitizeSingleRequest_unknownSchemaVersionTreatsAllKeysAsSensitive() throws Exception {
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(eq(TEMPLATE_ID), any()))
                .thenReturn(Optional.empty());

        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("mysteryField", "secret-probe"),
                null,
                "req-unknown",
                "idem-unknown",
                null
        );

        JsonNode root = objectMapper.readTree(
                sanitizer.sanitizeSingleRequest(request, "9.9.9", TEMPLATE_ID)
        );

        assertThat(root.toString()).doesNotContain("secret-probe");
        assertThat(root.get("variables").isEmpty()).isTrue();
        assertThat(root.get("redactedVariableKeys").get(0).asText()).isEqualTo("mysteryField");
    }

    private VariableSchemaEntity schema(String key, VariablePiiCategory piiCategory) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                VERSION_ID,
                key,
                VariableType.TEXT,
                false,
                null,
                null,
                null,
                null,
                piiCategory
        );
    }
}
