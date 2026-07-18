package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IBL-B1 / F9 — whitelisted paragraph spacing/indents must land in DOCX via POI.
 *
 * <p>BDD: docs/behavior/ibl-b1-direct-format-spacing.md (BDD-IBL-B1-001…006, 010).
 */
class StructuredContentDocxDirectFormatSpacingTest {

    private static final double TWIPS_PER_POINT = 20.0;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StructuredContentDocxWriter writer;

    @BeforeEach
    void setUp() {
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper);
    }

    @Test
    void bddIblB1001_spacingBeforeAndAfterAppliedInTwips() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph",
                  "directFormat":{"spacingBefore":12,"spacingAfter":6},
                  "children":[{"type":"textRun","value":"Spaced"}]}]}
                """;

        byte[] result = render(structured);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getSpacingBefore()).isEqualTo(pointsToTwips(12));
            assertThat(paragraph.getSpacingAfter()).isEqualTo(pointsToTwips(6));
        }
    }

    @Test
    void bddIblB1002_lineSpacingMultipleUsesAutoRule() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph",
                  "directFormat":{"lineSpacing":1.5},
                  "children":[{"type":"textRun","value":"Line spacing"}]}]}
                """;

        byte[] result = render(structured);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getSpacingBetween()).isEqualTo(1.5);
            assertThat(paragraph.getSpacingLineRule()).isEqualTo(LineSpacingRule.AUTO);
        }
    }

    @Test
    void bddIblB1003_indentsAppliedInTwips() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph",
                  "directFormat":{"leftIndent":24,"firstLineIndent":12,"rightIndent":6},
                  "children":[{"type":"textRun","value":"Indented"}]}]}
                """;

        byte[] result = render(structured);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getIndentationLeft()).isEqualTo(pointsToTwips(24));
            assertThat(paragraph.getIndentationFirstLine()).isEqualTo(pointsToTwips(12));
            assertThat(paragraph.getIndentationRight()).isEqualTo(pointsToTwips(6));
        }
    }

    @Test
    void bddIblB1004_fontKeysStillApplyOnRuns() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph",
                  "directFormat":{"fontFamily":"Arial","fontSize":14,"textColor":"#112233"},
                  "children":[{"type":"textRun","value":"Styled"}]}]}
                """;

        byte[] result = render(structured);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFRun run = document.getParagraphs().getFirst().getRuns().getFirst();
            assertThat(run.getFontFamily()).isEqualTo("Arial");
            assertThat(run.getFontSize()).isEqualTo(14);
            assertThat(run.getColor()).isEqualToIgnoringCase("112233");
        }
    }

    @Test
    void bddIblB1005_spacingOnlyWithoutFontKeys() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph",
                  "directFormat":{"spacingBefore":10},
                  "children":[{"type":"textRun","value":"Spacing only"}]}]}
                """;

        byte[] result = render(structured);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getSpacingBefore()).isEqualTo(pointsToTwips(10));
            assertThat(paragraph.getText()).isEqualTo("Spacing only");
        }
    }

    @Test
    void bddIblB1006_omittedSpacingKeysDoNotForceOverride() throws Exception {
        String withStyleOnly = """
                {"nodes":[{"type":"paragraph","styleRef":"BodyText",
                  "children":[{"type":"textRun","value":"Inherit"}]}]}
                """;
        String withEmptyDirectFormat = """
                {"nodes":[{"type":"paragraph","styleRef":"BodyText","directFormat":{},
                  "children":[{"type":"textRun","value":"Inherit"}]}]}
                """;

        byte[] baseline = render(withStyleOnly);
        byte[] emptyDf = render(withEmptyDirectFormat);

        try (XWPFDocument baselineDoc = StructuredContentDocxWriterTestSupport.openDocument(baseline);
                XWPFDocument emptyDfDoc = StructuredContentDocxWriterTestSupport.openDocument(emptyDf)) {
            XWPFParagraph baselineParagraph = baselineDoc.getParagraphs().getFirst();
            XWPFParagraph emptyDfParagraph = emptyDfDoc.getParagraphs().getFirst();
            assertThat(emptyDfParagraph.getSpacingBefore()).isEqualTo(baselineParagraph.getSpacingBefore());
            assertThat(emptyDfParagraph.getSpacingAfter()).isEqualTo(baselineParagraph.getSpacingAfter());
            assertThat(emptyDfParagraph.getIndentationLeft()).isEqualTo(baselineParagraph.getIndentationLeft());
            assertThat(emptyDfParagraph.getIndentationRight()).isEqualTo(baselineParagraph.getIndentationRight());
            assertThat(emptyDfParagraph.getIndentationFirstLine())
                    .isEqualTo(baselineParagraph.getIndentationFirstLine());
        }
    }

    @Test
    void bddIblB1010_sameWriterYieldsIdenticalSpacingForRepeatedRender() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph",
                  "directFormat":{"spacingBefore":12,"spacingAfter":6,"lineSpacing":1.5,"leftIndent":24},
                  "children":[{"type":"textRun","value":"Shared path"}]}]}
                """;

        byte[] previewPath = render(structured);
        byte[] runtimePath = render(structured);

        try (XWPFDocument previewDoc = StructuredContentDocxWriterTestSupport.openDocument(previewPath);
                XWPFDocument runtimeDoc = StructuredContentDocxWriterTestSupport.openDocument(runtimePath)) {
            XWPFParagraph preview = previewDoc.getParagraphs().getFirst();
            XWPFParagraph runtime = runtimeDoc.getParagraphs().getFirst();
            assertThat(runtime.getSpacingBefore()).isEqualTo(preview.getSpacingBefore());
            assertThat(runtime.getSpacingAfter()).isEqualTo(preview.getSpacingAfter());
            assertThat(runtime.getSpacingBetween()).isEqualTo(preview.getSpacingBetween());
            assertThat(runtime.getSpacingLineRule()).isEqualTo(preview.getSpacingLineRule());
            assertThat(runtime.getIndentationLeft()).isEqualTo(preview.getIndentationLeft());
        }
    }

    @Test
    void childTextRunDirectFormatOverridesParagraphFontDefaults() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph",
                  "directFormat":{"fontFamily":"Arial","fontSize":12,"spacingBefore":8},
                  "children":[{"type":"textRun","value":"Child",
                    "directFormat":{"fontFamily":"Courier New","fontSize":16}}]}]}
                """;

        byte[] result = render(structured);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getSpacingBefore()).isEqualTo(pointsToTwips(8));
            XWPFRun run = paragraph.getRuns().getFirst();
            assertThat(run.getFontFamily()).isEqualTo("Courier New");
            assertThat(run.getFontSize()).isEqualTo(16);
        }
    }

    private byte[] render(String structuredJson) throws Exception {
        return StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer,
                structuredJson,
                Map.of(),
                Map.of()
        );
    }

    private static int pointsToTwips(double points) {
        return (int) Math.round(points * TWIPS_PER_POINT);
    }
}
