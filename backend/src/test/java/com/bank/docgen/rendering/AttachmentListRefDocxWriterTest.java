package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.sharedkernel.document.structured.WriterUnsupportedStructuredNodeTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CE-K06c — attachmentListRef DOCX writer (BDD-CE-K06c-001…003).
 */
class AttachmentListRefDocxWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StructuredContentDocxWriter writer;

    @BeforeEach
    void setUp() {
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper);
    }

    @Test
    void writesOrderedNumberedList_andExitsUnsupportedSet() throws Exception {
        // BDD-CE-K06c-001
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        byte[] result = render(structured, Map.of(
                "ATTACHMENTS", List.of("Annex A — KYC pack", "Annex B — Fee schedule")
        ));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFParagraph> numbered = numberedParagraphs(document);
            assertThat(numbered).hasSize(2);
            assertThat(numbered.get(0).getText()).isEqualTo("Annex A — KYC pack");
            assertThat(numbered.get(1).getText()).isEqualTo("Annex B — Fee schedule");
            assertThat(numbered.get(0).getCTP().getPPr().getNumPr()).isNotNull();
            assertThat(numbered.get(1).getCTP().getPPr().getNumPr()).isNotNull();
        }
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("attachmentListRef")).isFalse();
        assertThat(WriterUnsupportedStructuredNodeTypes.jsonTypes()).isEmpty();
    }

    @Test
    void emptyArray_succeedsWithZeroNumberedParagraphs() throws Exception {
        // BDD-CE-K06c-002
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        byte[] result = render(structured, Map.of("ATTACHMENTS", List.of()));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(numberedParagraphs(document)).isEmpty();
        }
    }

    @Test
    void failsClosedOnMissingKey() {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of()))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.attachmentListPayloadMissing");
    }

    @Test
    void failsClosedOnNullValue() {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;
        Map<String, Object> variables = new HashMap<>();
        variables.put("ATTACHMENTS", null);

        assertThatThrownBy(() -> render(structured, variables))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.attachmentListPayloadMissing");
    }

    @Test
    void failsClosedOnNonArrayPayload() {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of("ATTACHMENTS", "Annex A")))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.attachmentListPayloadInvalid");
    }

    @Test
    void failsClosedOnObjectArrayPayload() {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        assertThatThrownBy(() -> render(
                        structured,
                        Map.of("ATTACHMENTS", List.of(Map.of("name", "Annex A")))))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.attachmentListPayloadInvalid");
    }

    @Test
    void failsClosedOnNonStringArrayElements() {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of("ATTACHMENTS", List.of(1, 2))))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.attachmentListPayloadInvalid");
    }

    @Test
    void failsClosedOnStringArrayWithNullElements() {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;
        Map<String, Object> variables = new HashMap<>();
        variables.put("ATTACHMENTS", new String[]{"Annex A", null});

        assertThatThrownBy(() -> render(structured, variables))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.attachmentListPayloadInvalid");
    }

    @Test
    void acceptsBlankAndWhitespaceStringElements() throws Exception {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        byte[] result = render(structured, Map.of("ATTACHMENTS", List.of("", "   ")));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFParagraph> numbered = numberedParagraphs(document);
            assertThat(numbered).hasSize(2);
            assertThat(numbered.get(0).getText()).isEmpty();
            assertThat(numbered.get(1).getText()).isEqualTo("   ");
        }
    }

    @Test
    void writesWhenNestedInConditionBlock() throws Exception {
        // BDD-CE-K06c-003
        String structured = """
                {"nodes":[{
                  "type":"conditionBlock",
                  "conditionExpression":"${show} == true",
                  "children":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]
                }]}
                """;

        byte[] result = render(structured, Map.of(
                "show", true,
                "ATTACHMENTS", List.of("Annex A", "Annex B")
        ));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFParagraph> numbered = numberedParagraphs(document);
            assertThat(numbered).hasSize(2);
            assertThat(numbered.get(0).getText()).isEqualTo("Annex A");
            assertThat(numbered.get(1).getText()).isEqualTo("Annex B");
        }
    }

    @Test
    void writesWhenInsidePinnedContentModule() throws Exception {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;
        Map<String, String> pinned = Map.of(
                "CLAUSE-1",
                "{\"nodes\":[{\"type\":\"attachmentListRef\",\"referenceKey\":\"ATTACHMENTS\"}]}"
        );

        byte[] result = StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer,
                structured,
                Map.of("ATTACHMENTS", List.of("Pinned annex")),
                pinned
        );

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFParagraph> numbered = numberedParagraphs(document);
            assertThat(numbered).hasSize(1);
            assertThat(numbered.getFirst().getText()).isEqualTo("Pinned annex");
        }
    }

    @Test
    void writesWhenNestedInLoopBlock() throws Exception {
        String structured = """
                {"nodes":[{
                  "type":"loopBlock",
                  "loopVariable":"items",
                  "children":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]
                }]}
                """;

        byte[] result = render(structured, Map.of(
                "items", List.of(Map.of("n", "1")),
                "ATTACHMENTS", List.of("Loop annex")
        ));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFParagraph> numbered = numberedParagraphs(document);
            assertThat(numbered).isNotEmpty();
            assertThat(numbered.getFirst().getText()).isEqualTo("Loop annex");
        }
    }

    private byte[] render(String structuredJson, Map<String, Object> variables) throws Exception {
        return StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer, structuredJson, variables, Map.of());
    }

    private static List<XWPFParagraph> numberedParagraphs(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getCTP().getPPr() != null
                        && paragraph.getCTP().getPPr().getNumPr() != null)
                .toList();
    }
}
