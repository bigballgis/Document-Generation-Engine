package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.domain.FidelityWarningCode;
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
}
