package com.bank.docgen.sharedkernel.document.fidelity;

/**
 * Cross-module fidelity warning codes shared by authoring validation and rendering output.
 */
public enum FidelityWarningCode {
    CONTROLLED_STYLE_FALLBACK,
    MASTER_STYLE_FALLBACK,
    LOW_RISK_PAGINATION_DIFFERENCE,
    PARTIAL_TABLE_LAYOUT_ADJUSTMENT,
    UNRESOLVED_VARIABLE,
    INVALID_CONDITION_EXPRESSION,
    MISSING_ANCHOR_CONTENT,
    UNSUPPORTED_NODE,
    IMAGE_SCALING_ADJUSTED,
    MISSING_STYLE_REFERENCE,
    INAPPLICABLE_STYLE,
    DIRECT_FORMAT_OUT_OF_WHITELIST,
    DIRECT_FORMAT_GLOBAL_LAYOUT,
    /** IBL-B1: whitelisted directFormat key present with illegal type/range. */
    DIRECT_FORMAT_INVALID_VALUE,
    NESTED_TABLE,
    UNRELIABLE_TABLE_LAYOUT,
    INVALID_TABLE_COMPONENT,
    SEAL_OUTSIDE_AUTHORIZED_AREA,
    SEAL_SCALING_NOT_ALLOWED,
    MISSING_REFERENCE_KEY,
    DUPLICATE_NUMBER,
    BROKEN_NUMBER_CROSS_REFERENCE,
    PDF_PAGE_NUMBER_STAMP_FAILED,
    /** CE-O01: PDFBox page-number stamp skipped to preserve PDF/A-2b archival bytes. */
    PDF_PAGE_NUMBER_STAMP_SKIPPED_FOR_PDFA,
    /** CE-C06: DOCX requested encryption.permissions; permissions apply only to PDF. */
    DOCX_PERMISSIONS_NOT_APPLIED
}
