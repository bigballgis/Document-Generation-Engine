package com.bank.docgen.rendering;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Stamps page numbers onto PDFs produced by LibreOffice headless conversion, which often
 * omits Word {@code PAGE} field evaluation in footers.
 */
public final class PdfPageNumberStamper {

    private static final float FONT_SIZE = 8f;
    private static final float BOTTOM_MARGIN = 28f;

    private PdfPageNumberStamper() {
    }

    public static byte[] stampPageNumbers(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                return pdfBytes;
            }
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                PDRectangle mediaBox = page.getMediaBox();
                String label = "Page " + (pageIndex + 1) + " of " + totalPages;
                float textWidth = font.getStringWidth(label) / 1000f * FONT_SIZE;
                float x = (mediaBox.getWidth() - textWidth) / 2f;
                try (PDPageContentStream contentStream = new PDPageContentStream(
                        document,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                )) {
                    contentStream.beginText();
                    contentStream.setFont(font, FONT_SIZE);
                    contentStream.newLineAtOffset(x, BOTTOM_MARGIN);
                    contentStream.showText(label);
                    contentStream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            return pdfBytes;
        }
    }
}
