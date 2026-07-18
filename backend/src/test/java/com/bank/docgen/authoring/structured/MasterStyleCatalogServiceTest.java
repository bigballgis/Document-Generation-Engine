package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MasterStyleCatalogServiceTest {

    private final MasterStyleCatalogService service = new MasterStyleCatalogService(new ObjectMapper());

    @Test
    void styleRef_resolvesToApprovedCatalog() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "styleRef": "BodyText",
                      "children": [{ "type": "textRun", "value": "hello" }]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void styleRef_notInCatalog_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "styleRef": "UnknownStyle",
                      "children": []
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.MISSING_STYLE_REFERENCE);
        assertThat(result.blockers().getFirst().messageKey())
                .isEqualTo(MasterStyleCatalogService.MESSAGE_KEY_MISSING_STYLE);
    }

    @Test
    void directFormat_outOfWhitelist_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "directFormat": { "fontWeight": "bold" },
                      "children": []
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.DIRECT_FORMAT_OUT_OF_WHITELIST);
        assertThat(result.blockers().getFirst().messageKey())
                .isEqualTo(MasterStyleCatalogService.MESSAGE_KEY_DIRECT_FORMAT_OUT_OF_WHITELIST);
    }

    @Test
    void directFormat_modifyingGlobalLayout_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "directFormat": { "pageMarginTop": "2cm" },
                      "children": []
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.DIRECT_FORMAT_GLOBAL_LAYOUT);
        assertThat(result.blockers().getFirst().messageKey())
                .isEqualTo(MasterStyleCatalogService.MESSAGE_KEY_DIRECT_FORMAT_GLOBAL_LAYOUT);
    }

    @Test
    void directFormat_negativeSpacing_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "directFormat": { "spacingBefore": -1 },
                      "children": []
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).isNotEmpty();
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.DIRECT_FORMAT_INVALID_VALUE);
        assertThat(result.blockers().getFirst().messageKey())
                .isEqualTo(MasterStyleCatalogService.MESSAGE_KEY_DIRECT_FORMAT_INVALID_VALUE);
    }

    @Test
    void directFormat_nonNumericSpacing_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "directFormat": { "spacingAfter": "twelve" },
                      "children": []
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).isNotEmpty();
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.DIRECT_FORMAT_INVALID_VALUE);
    }

    @Test
    void directFormat_nonPositiveLineSpacing_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "directFormat": { "lineSpacing": 0 },
                      "children": []
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).isNotEmpty();
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.DIRECT_FORMAT_INVALID_VALUE);
    }

    @Test
    void directFormat_validWhitelistSpacing_isAccepted() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "styleRef": "BodyText",
                      "directFormat": {
                        "spacingBefore": 12,
                        "spacingAfter": 6,
                        "lineSpacing": 1.5,
                        "leftIndent": 24,
                        "firstLineIndent": 12,
                        "rightIndent": 0
                      },
                      "children": [{ "type": "textRun", "value": "ok" }]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result =
                service.validate(json, service.loadForMaster(UUID.randomUUID()));

        assertThat(result.blockers()).isEmpty();
    }
}
