package com.bank.docgen.runtime.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.service.RuntimeFidelityWarningMapper;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-C03-001/002/003/007/008/009/010: batch item fidelityWarnings are full FidelityWarning
 * objects, not string warning codes; task JSON round-trip preserves objects; OpenAPI enum is honest.
 */
@ExtendWith(MockitoExtension.class)
class FidelityWarningContractTest {

    @Mock
    private MessageResolver messageResolver;

    private RuntimeFidelityWarningMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mapper = new RuntimeFidelityWarningMapper(messageResolver);
        lenient().when(messageResolver.resolveOrDefault(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void bddCeC03_001_batchSucceededItemSerializesFullFidelityWarningObjects() throws Exception {
        List<FidelityWarning> warnings = mapper.toWarnings(List.of(
                FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name()
        ));
        BatchResultItemView item = new BatchResultItemView(
                "ITEM-001",
                "SUCCEEDED",
                new OutputOptionsView("PDF", "ASYNC_TASK"),
                EncryptionSummaryView.disabled("PDF"),
                "DOC-1",
                warnings
        );

        JsonNode root = objectMapper.valueToTree(item);
        JsonNode fidelityWarnings = root.get("fidelityWarnings");

        assertThat(fidelityWarnings.isArray()).isTrue();
        assertThat(fidelityWarnings).hasSize(1);
        assertThat(fidelityWarnings.get(0).isObject()).isTrue();
        assertThat(fidelityWarnings.get(0).get("warningCode").asText())
                .isEqualTo("CONTROLLED_STYLE_FALLBACK");
        assertThat(fidelityWarnings.get(0).get("messageKey").asText())
                .isEqualTo("generation.warning.fidelity.controlledStyleFallback");
        assertThat(fidelityWarnings.get(0).get("message").isTextual()).isTrue();
        assertThat(fidelityWarnings.get(0).get("locationSummary").isTextual()).isTrue();
        assertThat(fidelityWarnings.get(0).get("detectedSummary").isTextual()).isTrue();
        assertThat(fidelityWarnings.get(0).get("recommendation").isTextual()).isTrue();
        assertThat(fidelityWarnings.get(0).get("sensitiveDataExcluded").asBoolean()).isTrue();
        assertThat(fidelityWarnings.get(0).isTextual()).isFalse();
    }

    @Test
    void bddCeC03_002_emptyWarningsSerializeAsEmptyArray() throws Exception {
        BatchResultItemView item = new BatchResultItemView(
                "ITEM-002",
                "SUCCEEDED",
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                EncryptionSummaryView.disabled("DOCX"),
                "DOC-2",
                mapper.toWarnings(List.of())
        );

        JsonNode fidelityWarnings = objectMapper.valueToTree(item).get("fidelityWarnings");
        assertThat(fidelityWarnings.isArray()).isTrue();
        assertThat(fidelityWarnings).isEmpty();
    }

    @Test
    void bddCeC03_007_failedItemUsesEmptyFidelityWarningsNotStringCodes() throws Exception {
        BatchResultItemView item = new BatchResultItemView(
                "ITEM-FAIL",
                "FAILED",
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                EncryptionSummaryView.disabled("DOCX"),
                null,
                List.of(),
                null
        );

        JsonNode fidelityWarnings = objectMapper.valueToTree(item).get("fidelityWarnings");
        assertThat(fidelityWarnings.isArray()).isTrue();
        assertThat(fidelityWarnings).isEmpty();
        assertThat(fidelityWarnings.toString()).doesNotContain("\"");
    }

    @Test
    void bddCeC03_008_sensitiveDataExcludedIsAlwaysTrue() {
        FidelityWarning warning = mapper.toWarnings(List.of(
                FidelityWarningCode.IMAGE_SCALING_ADJUSTED.name()
        )).getFirst();

        assertThat(warning.sensitiveDataExcluded()).isTrue();
        assertThat(warning.locationSummary()).doesNotContainIgnoringCase("password");
        assertThat(warning.detectedSummary()).doesNotContainIgnoringCase("secret");
        assertThat(warning.message()).doesNotContain("$");
    }

    @Test
    void bddCeC03_003_and_010_batchResultJsonRoundTripKeepsFullObjects() throws Exception {
        List<FidelityWarning> warnings = mapper.toWarnings(List.of(
                FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name(),
                FidelityWarningCode.IMAGE_SCALING_ADJUSTED.name()
        ));
        BatchResultView original = new BatchResultView(
                "BATCH-FW-1",
                new BatchSummaryView(1, 1, 1, 0, 0),
                List.of(new BatchResultItemView(
                        "ITEM-001",
                        "SUCCEEDED",
                        new OutputOptionsView("DOCX", "ASYNC_TASK"),
                        EncryptionSummaryView.disabled("DOCX"),
                        "DOC-1",
                        warnings
                ))
        );

        String json = objectMapper.writeValueAsString(original);
        BatchResultView replayed = objectMapper.readValue(json, BatchResultView.class);

        assertThat(json).contains("\"warningCode\"");
        assertThat(json).doesNotContain("\"fidelityWarnings\":[\"CONTROLLED");
        assertThat(replayed.items().getFirst().fidelityWarnings()).hasSize(2);
        assertThat(replayed.items().getFirst().fidelityWarnings().getFirst().warningCode())
                .isEqualTo("CONTROLLED_STYLE_FALLBACK");
        assertThat(replayed.items().getFirst().fidelityWarnings().getFirst().sensitiveDataExcluded())
                .isTrue();
        assertThat(replayed.items().getFirst().fidelityWarnings().get(1).warningCode())
                .isEqualTo("IMAGE_SCALING_ADJUSTED");
    }

    @Test
    void bddCeC03_009_javaFidelityWarningCodesAreDeclaredInOpenApiEnum() throws Exception {
        Path openApi = Path.of("docs/api/openapi-v1.yaml");
        if (!Files.isRegularFile(openApi)) {
            openApi = Path.of("../docs/api/openapi-v1.yaml");
        }
        assertThat(openApi).exists();

        String yaml = Files.readString(openApi);
        int schemaIdx = yaml.indexOf("FidelityWarningCode:");
        assertThat(schemaIdx).isGreaterThanOrEqualTo(0);
        int enumIdx = yaml.indexOf("enum:", schemaIdx);
        assertThat(enumIdx).isGreaterThan(schemaIdx);
        String enumBlock = yaml.substring(enumIdx, Math.min(enumIdx + 1200, yaml.length()));
        Matcher matcher = Pattern.compile("^\\s+- ([A-Z][A-Z0-9_]+)$", Pattern.MULTILINE).matcher(enumBlock);
        Set<String> openApiCodes = new LinkedHashSet<>();
        while (matcher.find()) {
            openApiCodes.add(matcher.group(1));
            if (openApiCodes.size() > 40) {
                break;
            }
        }

        Set<String> javaCodes = Arrays.stream(FidelityWarningCode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(openApiCodes)
                .as("OpenAPI FidelityWarningCode must declare every Java runtime/sharedkernel code")
                .containsAll(javaCodes);
        assertThat(openApiCodes).contains(
                "OPTIONAL_CONTENT_EMPTY",
                "LOW_RISK_TABLE_PAGE_BREAK",
                "CONTROLLED_STYLE_FALLBACK",
                "MASTER_STYLE_FALLBACK",
                "PDF_PAGE_NUMBER_STAMP_FAILED",
                "PDF_PAGE_NUMBER_STAMP_SKIPPED_FOR_PDFA",
                "DOCX_PERMISSIONS_NOT_APPLIED"
        );
    }
}
