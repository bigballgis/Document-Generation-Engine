package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.PdfArchivalProfile;

/**
 * Explicit PDF conversion options passed through the conversion pipeline (avoids ThreadLocal on async workers).
 */
public record PdfConversionOptions(
        PdfPageNumberStampPlan stampPlan,
        boolean pageNumberStampingEnabled,
        PdfArchivalProfile pdfArchivalProfile
) {

    public PdfConversionOptions {
        if (pdfArchivalProfile == null) {
            pdfArchivalProfile = PdfArchivalProfile.NONE;
        }
    }

    public static PdfConversionOptions stampingDisabled() {
        return stampingDisabled(PdfArchivalProfile.NONE);
    }

    public static PdfConversionOptions stampingDisabled(PdfArchivalProfile archivalProfile) {
        return new PdfConversionOptions(
                PdfPageNumberStampPlan.globalOnly(),
                false,
                archivalProfile == null ? PdfArchivalProfile.NONE : archivalProfile
        );
    }

    public static PdfConversionOptions stampingEnabled(PdfPageNumberStampPlan stampPlan) {
        return stampingEnabled(stampPlan, PdfArchivalProfile.NONE);
    }

    public static PdfConversionOptions stampingEnabled(
            PdfPageNumberStampPlan stampPlan,
            PdfArchivalProfile archivalProfile
    ) {
        return new PdfConversionOptions(
                stampPlan == null ? PdfPageNumberStampPlan.globalOnly() : stampPlan,
                true,
                archivalProfile == null ? PdfArchivalProfile.NONE : archivalProfile
        );
    }

    public boolean isPdfA2b() {
        return pdfArchivalProfile == PdfArchivalProfile.PDF_A_2B;
    }
}
