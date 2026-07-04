package com.bank.docgen.rendering;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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
        return stampPageNumbers(pdfBytes, PdfPageNumberStampPlan.globalOnly()).pdfBytes();
    }

    public static PdfPageStampResult stampPageNumbers(byte[] pdfBytes, PdfPageNumberStampPlan plan) {
        PdfPageNumberStampPlan resolvedPlan = plan == null ? PdfPageNumberStampPlan.globalOnly() : plan;
        try (PDDocument document = Loader.loadPDF(pdfBytes);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                return PdfPageStampResult.success(pdfBytes);
            }
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<Integer> sectionStarts = normalizeSectionStarts(resolvedPlan.sectionStartPages(), totalPages);
            PdfPageNumberStampPlan normalizedPlan = new PdfPageNumberStampPlan(
                    resolvedPlan.dualPageNumbersEnabled(),
                    sectionStarts
            );
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                int pageNumber = pageIndex + 1;
                PDPage page = document.getPage(pageIndex);
                PDRectangle mediaBox = page.getMediaBox();
                String label = buildLabel(pageNumber, totalPages, normalizedPlan);
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
            return PdfPageStampResult.success(output.toByteArray());
        } catch (IOException ex) {
            return PdfPageStampResult.failure(pdfBytes);
        }
    }

    private static List<Integer> normalizeSectionStarts(List<Integer> sectionStartPages, int totalPages) {
        if (sectionStartPages == null || sectionStartPages.isEmpty()) {
            return List.of(1);
        }
        return sectionStartPages.stream()
                .filter(start -> start >= 1 && start <= totalPages)
                .distinct()
                .sorted()
                .toList();
    }

    private static String buildLabel(int pageNumber, int totalPages, PdfPageNumberStampPlan plan) {
        if (!plan.dualPageNumbersEnabled()) {
            return "Page " + pageNumber + " of " + totalPages;
        }
        int sectionPage = plan.sectionPageNumber(pageNumber);
        int sectionTotal = plan.sectionPageCount(pageNumber, totalPages);
        return "Section Page " + sectionPage + " of " + sectionTotal
                + "  |  Document Page " + pageNumber + " of " + totalPages;
    }
}
