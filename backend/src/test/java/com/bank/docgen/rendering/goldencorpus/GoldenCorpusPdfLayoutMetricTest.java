package com.bank.docgen.rendering.goldencorpus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * IBL-C1 / F17 — PDFBox page-count + text-position layout metrics (not pixel).
 */
class GoldenCorpusPdfLayoutMetricTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> PDF_TYPES = Set.of(
            "TEXT_CONTAINS",
            "TEXT_NOT_CONTAINS",
            "PAGE_COUNT",
            "TEXT_POSITION"
    );

    private final GoldenCorpusAssertionLoader assertionLoader = new GoldenCorpusAssertionLoader(OBJECT_MAPPER);
    private final GoldenCorpusPdfAssertor pdfAssertor = new GoldenCorpusPdfAssertor();

    @Test
    void loaderAcceptsPageCountAndTextPositionTypes(@TempDir Path tempDir) throws Exception {
        Path assertionFile = tempDir.resolve("pdf-assertions.json");
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode assertions = root.putArray("assertions");
        ObjectNode pageCount = assertions.addObject();
        pageCount.put("type", "PAGE_COUNT");
        pageCount.put("equals", 1);
        ObjectNode textPosition = assertions.addObject();
        textPosition.put("type", "TEXT_POSITION");
        textPosition.put("substring", "MARKER");
        textPosition.put("pageIndex", 0);
        textPosition.put("xMin", 40.0);
        textPosition.put("xMax", 120.0);
        textPosition.put("yMin", 700.0);
        textPosition.put("yMax", 800.0);
        Files.writeString(assertionFile, OBJECT_MAPPER.writeValueAsString(root));

        assertThat(assertionLoader.loadAndValidate(assertionFile, PDF_TYPES).path("assertions")).hasSize(2);
    }

    @Test
    void loaderStillRejectsPixelTypes(@TempDir Path tempDir) throws Exception {
        Path assertionFile = tempDir.resolve("pdf-assertions.json");
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode assertions = root.putArray("assertions");
        ObjectNode bad = assertions.addObject();
        bad.put("type", "PIXEL_DIFF");
        Files.writeString(assertionFile, OBJECT_MAPPER.writeValueAsString(root));

        assertThatThrownBy(() -> assertionLoader.loadAndValidate(assertionFile, PDF_TYPES))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("PIXEL");
    }

    @Test
    void pageCountAndTextPositionPassOnKnownPdf() throws Exception {
        byte[] pdf = synthesizeMarkerPdf("LAYOUT_MARKER", 50f, 750f, 1);
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode assertions = root.putArray("assertions");
        ObjectNode pageCount = assertions.addObject();
        pageCount.put("type", "PAGE_COUNT");
        pageCount.put("equals", 1);
        ObjectNode textPosition = assertions.addObject();
        textPosition.put("type", "TEXT_POSITION");
        textPosition.put("substring", "LAYOUT_MARKER");
        textPosition.put("pageIndex", 0);
        // PDFBox TextPosition YDirAdj is top-down on the page (A4≈842: y=750 → ≈92).
        textPosition.put("xMin", 40.0);
        textPosition.put("xMax", 80.0);
        textPosition.put("yMin", 80.0);
        textPosition.put("yMax", 110.0);

        assertThatCode(() -> pdfAssertor.assertPlainPdf(pdf, root)).doesNotThrowAnyException();
    }

    @Test
    void pageCountFailsWhenNotEqual() throws Exception {
        byte[] pdf = synthesizeMarkerPdf("LAYOUT_MARKER", 50f, 750f, 1);
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode assertions = root.putArray("assertions");
        ObjectNode pageCount = assertions.addObject();
        pageCount.put("type", "PAGE_COUNT");
        pageCount.put("equals", 2);

        assertThatThrownBy(() -> pdfAssertor.assertPlainPdf(pdf, root))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("PAGE_COUNT");
    }

    @Test
    void textPositionFailsOutsideBox() throws Exception {
        byte[] pdf = synthesizeMarkerPdf("LAYOUT_MARKER", 50f, 750f, 1);
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode assertions = root.putArray("assertions");
        ObjectNode textPosition = assertions.addObject();
        textPosition.put("type", "TEXT_POSITION");
        textPosition.put("substring", "LAYOUT_MARKER");
        textPosition.put("pageIndex", 0);
        textPosition.put("xMin", 200.0);
        textPosition.put("xMax", 300.0);
        textPosition.put("yMin", 100.0);
        textPosition.put("yMax", 200.0);

        assertThatThrownBy(() -> pdfAssertor.assertPlainPdf(pdf, root))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("TEXT_POSITION");
    }

    private static byte[] synthesizeMarkerPdf(String text, float x, float y, int pages) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(x, y);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
