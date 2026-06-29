package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class NumberingServiceTest {

    private final NumberingService service = new NumberingService(new ObjectMapper());

    @Test
    void numbering_afterLoopRender_isDeterministic() {
        String json = """
                {
                  "numberingScheme": "MULTILEVEL-1",
                  "nodes": [
                    { "type": "sectionHeading", "numbering": { "level": 1 } },
                    {
                      "type": "loopBlock",
                      "loopVariable": "items",
                      "validationIterations": 2,
                      "children": [
                        { "type": "sectionHeading", "numbering": { "level": 2 } }
                      ]
                    }
                  ]
                }
                """;

        assertThat(service.computeDisplayNumbers(json)).containsExactly("1", "1.1", "1.2");
        assertThat(service.computeDisplayNumbers(json)).isEqualTo(service.computeDisplayNumbers(json));
    }

    @Test
    void duplicateNumber_isBlocker() {
        String json = """
                {
                  "nodes": [
                    { "type": "sectionHeading", "numbering": { "level": 1, "displayNumber": "1.1" } },
                    { "type": "sectionHeading", "numbering": { "level": 1, "displayNumber": "1.1" } }
                  ]
                }
                """;

        NumberingValidationResult result = service.validateStructuredContent(json);

        assertThat(result.fidelity().blockers()).hasSize(1);
        assertThat(result.fidelity().blockers().getFirst().code()).isEqualTo(FidelityWarningCode.DUPLICATE_NUMBER);
        assertThat(result.fidelity().blockers().getFirst().messageKey())
                .isEqualTo(NumberingService.MESSAGE_KEY_DUPLICATE_NUMBER);
    }

    @Test
    void brokenCrossReference_isBlocker() {
        String json = """
                {
                  "nodes": [
                    { "type": "sectionHeading", "numbering": { "level": 1, "displayNumber": "1" } },
                    {
                      "type": "paragraph",
                      "numberingCrossRef": { "targetNumber": "9.9" },
                      "children": [{ "type": "textRun", "value": "See clause 9.9" }]
                    }
                  ]
                }
                """;

        NumberingValidationResult result = service.validateStructuredContent(json);

        assertThat(result.fidelity().blockers()).hasSize(1);
        assertThat(result.fidelity().blockers().getFirst().code())
                .isEqualTo(FidelityWarningCode.BROKEN_NUMBER_CROSS_REFERENCE);
        assertThat(result.fidelity().blockers().getFirst().messageKey())
                .isEqualTo(NumberingService.MESSAGE_KEY_BROKEN_CROSS_REFERENCE);
    }
}
