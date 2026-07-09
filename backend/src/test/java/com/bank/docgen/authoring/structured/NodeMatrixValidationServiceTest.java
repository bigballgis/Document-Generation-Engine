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
