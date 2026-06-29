package com.bank.docgen.authoring.structured;

/**
 * Caller-supplied render overrides are ignored at runtime; retained for contract evolution (P18-T08).
 */
public record CallerRenderOverride(
        String imageScalingPolicy,
        String pdfConversionPolicy,
        String tablePaginationPolicy,
        String numberingBehavior,
        String fidelityPolicy
) {

    public static CallerRenderOverride empty() {
        return new CallerRenderOverride(null, null, null, null, null);
    }

    public boolean isEmpty() {
        return imageScalingPolicy == null
                && pdfConversionPolicy == null
                && tablePaginationPolicy == null
                && numberingBehavior == null
                && fidelityPolicy == null;
    }
}
