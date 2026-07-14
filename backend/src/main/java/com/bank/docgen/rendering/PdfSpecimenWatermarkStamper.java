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
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

/**
 * Diagonal {@code SPECIMEN} watermark via PDFBox post-process (CE-G02). Pattern aligns with
 * {@link PdfPageNumberStamper} (load → append content stream → save) but is fail-closed.
 */
public final class PdfSpecimenWatermarkStamper {

    public static final String WATERMARK_TEXT = "SPECIMEN";

    private static final float FONT_SIZE = 64f;
    private static final float ROTATION_RADIANS = (float) Math.toRadians(45);
    private static final float OPACITY = 0.35f;

    private PdfSpecimenWatermarkStamper() {
    }

    public static byte[] apply(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                throw new RenderingOperationException("api.error.rendering.generationFailed");
            }
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
            graphicsState.setNonStrokingAlphaConstant(OPACITY);
            float textWidth = font.getStringWidth(WATERMARK_TEXT) / 1000f * FONT_SIZE;
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                PDRectangle mediaBox = page.getMediaBox();
                float centerX = mediaBox.getWidth() / 2f;
                float centerY = mediaBox.getHeight() / 2f;
                try (PDPageContentStream contentStream = new PDPageContentStream(
                        document,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                )) {
                    contentStream.setGraphicsStateParameters(graphicsState);
                    contentStream.setNonStrokingColor(0.55f, 0.55f, 0.55f);
                    contentStream.beginText();
                    contentStream.setFont(font, FONT_SIZE);
                    contentStream.setTextMatrix(Matrix.getRotateInstance(ROTATION_RADIANS, centerX, centerY));
                    contentStream.newLineAtOffset(-textWidth / 2f, -FONT_SIZE / 2f);
                    contentStream.showText(WATERMARK_TEXT);
                    contentStream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RenderingOperationException("api.error.rendering.generationFailed");
        }
    }
}
