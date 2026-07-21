package com.bank.docgen.documentbrand.domain;

/**
 * Document-brand resolution result.
 *
 * <p>After ADR-0071 / SYS-NORM Wave 6, catalog resolve is retired: runtime uses
 * {@link #letterheadNeutral()} so letterhead/logo/seal come from Letterhead (master)
 * bindings. Orthogonal to UI BrandPreset ({@code REDBC}/{@code GREENBC}).
 */
public record ResolvedDocumentBrand(
        String legalEntityCode,
        String documentBrandCode,
        String logoObjectRef,
        String defaultSealObjectRef,
        String letterheadLegalName
) {

    /** No catalog assets — Letterhead (master) / existing non-catalog bindings apply. */
    public static ResolvedDocumentBrand letterheadNeutral() {
        return new ResolvedDocumentBrand(null, null, null, null, null);
    }
}
