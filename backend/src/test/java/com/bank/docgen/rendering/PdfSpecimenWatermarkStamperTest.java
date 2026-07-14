package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-G02-PDF-001 / PDF-002 / X-001 — diagonal SPECIMEN, fail-closed.
 */
class PdfSpecimenWatermarkStamperTest {

    @Test
    void stampsSpecimenOnEveryPageExtractably() throws Exception {
        byte[] sourcePdf = buildPlainPdf(3, "Formal page body");

        byte[] stamped = PdfSpecimenWatermarkStamper.apply(sourcePdf);

        try (PDDocument document = Loader.loadPDF(stamped)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= 3; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertThat(stripper.getText(document).replaceAll("\\s+", "")).contains("SPECIMEN");
            }
        }
    }

    @Test
    void failsClosedOnCorruptPdf() {
        assertThatThrownBy(() -> PdfSpecimenWatermarkStamper.apply(new byte[] {1, 2, 3}))
                .isInstanceOf(RenderingOperationException.class)
                .extracting(ex -> ((RenderingOperationException) ex).messageKey())
                .isEqualTo("api.error.rendering.generationFailed");
    }

    @Test
    void doesNotReturnOriginalBytesOnFailure() {
        byte[] corrupt = new byte[] {0x25, 0x50, 0x44, 0x46}; // "%PDF" truncated
        assertThatThrownBy(() -> PdfSpecimenWatermarkStamper.apply(corrupt))
                .isInstanceOf(RenderingOperationException.class);
    }

    private static byte[] buildPlainPdf(int pages, String bodyPrefix) throws Exception {
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int pageNumber = 1; pageNumber <= pages; pageNumber++) {
                PDPage page = new PDPage(PDRectangle.A4);
                source.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(source, page)) {
                    contentStream.beginText();
                    contentStream.setFont(font, 12);
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText(bodyPrefix + " " + pageNumber);
                    contentStream.endText();
                }
            }
            source.save(output);
            return output.toByteArray();
        }
    }
}
