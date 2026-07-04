package com.bank.docgen.rendering;

/**
 * Explicit PDF conversion options passed through the conversion pipeline (avoids ThreadLocal on async workers).
 */
public record PdfConversionOptions(
        PdfPageNumberStampPlan stampPlan,
        boolean pageNumberStampingEnabled
) {

    public static PdfConversionOptions stampingDisabled() {
        return new PdfConversionOptions(PdfPageNumberStampPlan.globalOnly(), false);
    }

    public static PdfConversionOptions stampingEnabled(PdfPageNumberStampPlan stampPlan) {
        return new PdfConversionOptions(
                stampPlan == null ? PdfPageNumberStampPlan.globalOnly() : stampPlan,
                true
        );
    }
}
