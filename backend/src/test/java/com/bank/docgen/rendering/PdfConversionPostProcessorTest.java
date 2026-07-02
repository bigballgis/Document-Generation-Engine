package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
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

class PdfConversionPostProcessorTest {

    @Test
    void leavesPdfUntouchedWhenStampingDisabled() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(false);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        byte[] sourcePdf = samplePdf();

        byte[] finished = processor.finishPdf(sourcePdf);

        try (PDDocument document = Loader.loadPDF(finished)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Body page 1");
            assertThat(text).doesNotContain("Page 1 of 1");
        }
    }

    @Test
    void stampsPdfWhenStampingEnabled() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(true);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        byte[] sourcePdf = samplePdf();

        byte[] finished = processor.finishPdf(sourcePdf);

        try (PDDocument document = Loader.loadPDF(finished)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Page 1 of 1");
        }
    }

    private static byte[] samplePdf() throws Exception {
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            source.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream contentStream = new PDPageContentStream(source, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Body page 1");
                contentStream.endText();
            }
            source.save(output);
            return output.toByteArray();
        }
    }
}
