package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class StructuredContentSchemaTest {

    private final StructuredContentSchemaValidator validator =
            new StructuredContentSchemaValidator(new ObjectMapper());

    @Test
    void parse_supportedNodes_succeeds() {
        String json = """
                {
                  "schemaVersion": "1.0",
                  "nodes": [
                    {
                      "type": "sectionHeading",
                      "children": [
                        { "type": "text", "value": "Heading" }
                      ]
                    },
                    {
                      "type": "paragraph",
                      "children": [
                        { "type": "textRun", "value": "Hello " },
                        { "type": "variable", "key": "customerName" },
                        { "type": "lineBreak" },
                        { "type": "emphasis", "children": [{ "type": "text", "value": "emph" }] }
                      ]
                    },
                    { "type": "list", "children": [{ "type": "paragraph", "children": [{ "type": "text", "value": "item" }] }] },
                    { "type": "conditionBlock", "children": [{ "type": "paragraph", "children": [{ "type": "text", "value": "when true" }] }] },
                    { "type": "loopBlock", "children": [{ "type": "paragraph", "children": [{ "type": "text", "value": "each row" }] }] },
                    { "type": "tableComponentRef", "tableComponentRef": "TABLE-1" },
                    { "type": "contentModuleRef", "referenceKey": "CLAUSE-1" },
                    { "type": "imageRef", "imageRef": "IMG-1" },
                    { "type": "qrBarcodeRef", "referenceKey": "QR-1" },
                    { "type": "sealRef", "referenceKey": "SEAL-1" },
                    { "type": "attachmentListRef", "referenceKey": "ATT-1" },
                    { "type": "styleRef", "styleRef": "BodyText" }
                  ]
                }
                """;

        assertThatCode(() -> validator.validate(json)).doesNotThrowAnyException();
    }

    @Test
    void parse_unknownNodeType_isRejected() {
        String json = """
                {"nodes":[{"type":"marquee","value":"bad"}]}
                """;

        assertThatThrownBy(() -> validator.validate(json))
                .isInstanceOf(StructuredContentSchemaException.class)
                .extracting(ex -> ((StructuredContentSchemaException) ex).messageKey())
                .isEqualTo(StructuredContentSchemaValidator.MESSAGE_KEY_UNKNOWN_NODE);
    }

    @Test
    void parse_forbiddenConstruct_isRejected() {
        String scriptInText = """
                {"nodes":[{"type":"paragraph","children":[{"type":"text","value":"<script>alert(1)</script>"}]}]}
                """;
        assertThatThrownBy(() -> validator.validate(scriptInText))
                .isInstanceOf(StructuredContentSchemaException.class)
                .extracting(ex -> ((StructuredContentSchemaException) ex).messageKey())
                .isEqualTo(StructuredContentSchemaValidator.MESSAGE_KEY_FORBIDDEN);

        String htmlField = """
                {"nodes":[{"type":"paragraph","html":"<div>raw</div>","children":[]}]}
                """;
        assertThatThrownBy(() -> validator.validate(htmlField))
                .isInstanceOf(StructuredContentSchemaException.class)
                .extracting(ex -> ((StructuredContentSchemaException) ex).messageKey())
                .isEqualTo(StructuredContentSchemaValidator.MESSAGE_KEY_FORBIDDEN);
    }
}
