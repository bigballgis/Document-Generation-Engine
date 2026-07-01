package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

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

class PdfPageNumberStamperTest {

    @Test
    void stampsPageNumbersOnEachPage() throws Exception {
        byte[] sourcePdf;
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int pageNumber = 1; pageNumber <= 2; pageNumber++) {
                PDPage page = new PDPage(PDRectangle.A4);
                source.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(source, page)) {
                    contentStream.beginText();
                    contentStream.setFont(font, 12);
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText("Body page " + pageNumber);
                    contentStream.endText();
                }
            }
            source.save(output);
            sourcePdf = output.toByteArray();
        }

        byte[] stamped = PdfPageNumberStamper.stampPageNumbers(sourcePdf);

        try (PDDocument document = Loader.loadPDF(stamped)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Page 1 of 2").contains("Page 2 of 2");
        }
    }
}
