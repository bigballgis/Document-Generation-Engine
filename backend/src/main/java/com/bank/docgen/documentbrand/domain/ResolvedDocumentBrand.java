package com.bank.docgen.documentbrand.domain;

/**
 * Deterministic document-brand resolution result (ADR-0065 / IBL-E4).
 * Orthogonal to UI BrandPreset ({@code REDBC}/{@code GREENBC}).
 */
public record ResolvedDocumentBrand(
        String legalEntityCode,
        String documentBrandCode,
        String logoObjectRef,
        String defaultSealObjectRef,
        String letterheadLegalName
) {
}
