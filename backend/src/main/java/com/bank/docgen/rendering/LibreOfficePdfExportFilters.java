package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.PdfArchivalProfile;

/**
 * LibreOffice {@code --convert-to} arguments for conventional PDF vs PDF/A-2b (CE-O01 / ADR-0058).
 *
 * <p>PDF/A-2b uses {@code writer_pdf_Export} FilterData {@code SelectPdfVersion=2}
 * (LibreOffice: 0=PDF1.4, 1=PDF/A-1b, 2=PDF/A-2b, 3=PDF/A-3b).
 */
public final class LibreOfficePdfExportFilters {

    public static final String CONVENTIONAL_PDF = "pdf";

    /**
     * Single ProcessBuilder argument — JSON FilterData is not shell-escaped (ProcessBuilder argv).
     */
    public static final String PDF_A_2B =
            "pdf:writer_pdf_Export:{\"SelectPdfVersion\":{\"type\":\"long\",\"value\":\"2\"}}";

    private LibreOfficePdfExportFilters() {
    }

    public static String convertToArgument(PdfArchivalProfile profile) {
        if (profile == PdfArchivalProfile.PDF_A_2B) {
            return PDF_A_2B;
        }
        return CONVENTIONAL_PDF;
    }
}
