package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

/**
 * IBL-E7 / #134 / F15 spike inventory probe — documents current LTR-only OOXML emission.
 *
 * <p>Not a product RTL implementation. Asserts that structured-content DOCX output does
 * <strong>not</strong> emit paragraph/run bidi markers today, and that CTPPr exposes a low-level
 * {@code bidi} seam (XMLBeans) the platform does not yet wire. BDD: not-applicable (spike).
 */
class RtlBidiInventoryProbeTest {

    private static final String ARABIC_SAMPLE = "خطاب بنكي";
    private static final String MIXED_SAMPLE = ARABIC_SAMPLE + " Account 12345";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StructuredContentDocxWriter writer;

    @BeforeEach
    void setUp() {
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper);
    }

    @Test
    void structuredWriterDoesNotEmitParagraphOrRunBidiMarkersForArabicText() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"%s"}
                ]}]}
                """.formatted(MIXED_SAMPLE);

        byte[] docx = StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer,
                structured,
                Map.of(),
                Map.of()
        );

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(docx)) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getText()).contains(ARABIC_SAMPLE).contains("Account 12345");

            CTPPr pPr = paragraph.getCTP().getPPr();
            if (pPr != null) {
                assertThat(pPr.isSetBidi())
                        .as("structured writer must not set w:bidi on paragraph properties today")
                        .isFalse();
            }
        }

        String documentXml = readZipPart(docx, "word/document.xml").toLowerCase(Locale.ROOT);
        assertThat(documentXml)
                .as("OOXML must not contain paragraph/run RTL/bidi markers from structured writer")
                .doesNotContain("<w:bidi")
                .doesNotContain("<w:rtl");
        assertThat(documentXml).contains(ARABIC_SAMPLE.toLowerCase(Locale.ROOT));
    }

    @Test
    void poiCtpPrExposesLowLevelBidiSeamNotUsedByPlatform() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            if (paragraph.getCTP().getPPr() == null) {
                paragraph.getCTP().addNewPPr();
            }
            CTPPr pPr = paragraph.getCTP().getPPr();
            assertThat(pPr)
                    .as("POI CTPPr must expose addNewBidi for a future OOXML wire-up (not product RTL)")
                    .isNotNull();
            pPr.addNewBidi();
            assertThat(pPr.isSetBidi()).isTrue();
        }
    }

    private static String readZipPart(byte[] docxBytes, String partName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (partName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("DOCX part not found: " + partName);
    }
}
