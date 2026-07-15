package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NodeMatrixValidationServiceTest {

    private final NodeMatrixValidationService service =
            new NodeMatrixValidationService(new ObjectMapper());

    @Test
    void missingVariableRef_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "children": [
                        { "type": "variable", "key": "customerName" }
                      ]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of("otherKey"));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().severity()).isEqualTo(StructuredContentFidelitySeverity.BLOCKER);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.UNRESOLVED_VARIABLE);
        assertThat(result.blockers().getFirst().messageKey())
                .isEqualTo(NodeMatrixValidationService.MESSAGE_KEY_UNRESOLVED_VARIABLE);
        assertThat(result.blockers().getFirst().location()).isEqualTo("nodes[0].children[0]");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void unsupportedNode_isBlocker() {
        String json = """
                {"nodes":[{"type":"marquee","value":"bad"}]}
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of());

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.UNSUPPORTED_NODE);
        assertThat(result.blockers().getFirst().messageKey())
                .isEqualTo(NodeMatrixValidationService.MESSAGE_KEY_UNSUPPORTED_NODE);
        assertThat(result.blockers().getFirst().location()).isEqualTo("nodes[0]");
    }

    @Test
    void qrBarcodeRef_isNotWriterUnsupportedBlocker() {
        // CE-K06b — qrBarcodeRef exits writer-unsupported set (BDD-CE-K06b-006)
        String json = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of());

        assertThat(result.blockers()).isEmpty();
    }

    @Test
    void attachmentListRef_isNotWriterUnsupportedBlocker() {
        // CE-K06c — attachmentListRef exits writer-unsupported set (BDD-CE-K06c-003)
        String json = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of());

        assertThat(result.blockers()).isEmpty();
    }

    @Test
    void attachmentListRefNestedInCondition_isNotWriterUnsupportedBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "conditionBlock",
                      "conditionExpression": "${show} == true",
                      "children": [
                        { "type": "attachmentListRef", "referenceKey": "ATTACHMENTS" }
                      ]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of("show"));

        assertThat(result.blockers()).isEmpty();
    }

    @Test
    void countUnsupportedNodeBlockers_countsUnknownOnly_afterK06c() {
        assertThat(service.countUnsupportedNodeBlockers(
                "{\"nodes\":[{\"type\":\"qrBarcodeRef\",\"referenceKey\":\"QR-1\"}]}"))
                .isZero();
        assertThat(service.countUnsupportedNodeBlockers(
                "{\"nodes\":[{\"type\":\"attachmentListRef\",\"referenceKey\":\"ATTACHMENTS\"}]}"))
                .isZero();
        assertThat(service.countUnsupportedNodeBlockers(
                "{\"nodes\":[{\"type\":\"rawHtml\",\"value\":\"x\"}]}"))
                .isEqualTo(1);
        assertThat(service.countUnsupportedNodeBlockers(
                "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"textRun\",\"value\":\"ok\"}]}]}"))
                .isZero();
    }

    @Test
    void validation_excludesSensitiveData() {
        String sensitivePlaintext = "secret-value-12345";
        String json = """
                {
                  "nodes": [
                    {
                      "type": "paragraph",
                      "children": [
                        { "type": "textRun", "value": "%s" },
                        { "type": "variable", "key": "missingKey" }
                      ]
                    }
                  ]
                }
                """.formatted(sensitivePlaintext);

        StructuredContentValidationResult result = service.validate(json, Set.of("declaredOnly"));

        assertThat(result.blockers()).hasSize(1);
        StructuredContentFidelityIssue blocker = result.blockers().getFirst();
        assertThat(blocker.detectionSummary()).doesNotContain(sensitivePlaintext);
        assertThat(blocker.suggestion()).doesNotContain(sensitivePlaintext);
        assertThat(blocker.detectionSummary()).contains("missingKey");
    }

    @Test
    void malformedConditionExpression_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "conditionBlock",
                      "conditionExpression": "${x} === true",
                      "children": [{ "type": "textRun", "value": "Hidden" }]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of("x"));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.INVALID_CONDITION_EXPRESSION);
        assertThat(result.blockers().getFirst().messageKey())
                .isEqualTo(NodeMatrixValidationService.MESSAGE_KEY_INVALID_CONDITION_EXPRESSION);
        assertThat(result.blockers().getFirst().location()).isEqualTo("nodes[0]");
    }

    @Test
    void undeclaredVariableInConditionExpression_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "conditionBlock",
                      "conditionExpression": "${missingVar} != null",
                      "children": [{ "type": "textRun", "value": "Hidden" }]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of("customerName"));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.UNRESOLVED_VARIABLE);
        assertThat(result.blockers().getFirst().location()).isEqualTo("nodes[0]");
    }

    @Test
    void validRichConditionExpression_passesValidation() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "conditionBlock",
                      "conditionExpression": "${customerName} != null && ${amount} >= 0",
                      "children": [{ "type": "textRun", "value": "Shown" }]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of("customerName", "amount"));

        assertThat(result.blockers()).isEmpty();
    }

    @Test
    void undeclaredLoopVariable_isBlocker() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "loopBlock",
                      "loopVariable": "undeclaredItems",
                      "children": [{ "type": "textRun", "value": "Row" }]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of("items"));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.UNRESOLVED_VARIABLE);
        assertThat(result.blockers().getFirst().location()).isEqualTo("nodes[0]");
    }

    @Test
    void nestedMalformedConditionExpression_reportsInnerLocation() {
        String json = """
                {
                  "nodes": [
                    {
                      "type": "conditionBlock",
                      "conditionExpression": "${show} == true",
                      "children": [
                        {
                          "type": "conditionBlock",
                          "conditionExpression": "${inner} === true",
                          "children": [{ "type": "textRun", "value": "Nested" }]
                        }
                      ]
                    }
                  ]
                }
                """;

        StructuredContentValidationResult result = service.validate(json, Set.of("show", "inner"));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.blockers().getFirst().code()).isEqualTo(FidelityWarningCode.INVALID_CONDITION_EXPRESSION);
        assertThat(result.blockers().getFirst().location()).isEqualTo("nodes[0].children[0]");
    }
}
