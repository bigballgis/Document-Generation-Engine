package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import java.util.Optional;

/**
 * Result of optional PDF page-number stamping; carries a fidelity warning when stamping fails.
 */
public record PdfPageStampResult(byte[] pdfBytes, Optional<FidelityWarningCode> warning) {

    public static PdfPageStampResult success(byte[] pdfBytes) {
        return new PdfPageStampResult(pdfBytes, Optional.empty());
    }

    public static PdfPageStampResult failure(byte[] originalPdfBytes) {
        return new PdfPageStampResult(
                originalPdfBytes,
                Optional.of(FidelityWarningCode.PDF_PAGE_NUMBER_STAMP_FAILED)
        );
    }

    /** CE-O01: skip PDFBox stamp when archival PDF/A must be preserved. */
    public static PdfPageStampResult skippedForArchival(byte[] pdfBytes) {
        return new PdfPageStampResult(
                pdfBytes,
                Optional.of(FidelityWarningCode.PDF_PAGE_NUMBER_STAMP_SKIPPED_FOR_PDFA)
        );
    }
}
