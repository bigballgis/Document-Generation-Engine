package com.bank.docgen.template.domain;

public enum PublishGateCheckCode {
    ANCHOR_INTEGRITY,
    VARIABLE_SCHEMA,
    RULE_BOUNDS,
    TEST_RESULTS,
    PREVIEW_PRESENT,
    CHANGE_DIFF,
    APPROVAL_SUMMARY,
    COVERAGE_THRESHOLDS,
    API_POLICY,
    CONTENT_MODULE_REFERENCES,
    CONTENT_MODULE_EFFECTIVE_EXPIRED,
    /** IBL-E5 / ADR-0066: pinned content-module version effectiveFrom not yet reached. */
    CONTENT_MODULE_EFFECTIVE_NOT_STARTED,
    /** IBL-E1 / ADR-0061: pinned content-module locale incompatible with template locale. */
    CONTENT_MODULE_LOCALE_MISMATCH,
    /** IBL-E6 / ADR-0067: nesting cycle in pinned CM closure. */
    CONTENT_MODULE_NESTING_CYCLE,
    /** IBL-E6 / ADR-0067: nesting depth &gt; 8 in pinned CM closure. */
    CONTENT_MODULE_NESTING_DEPTH_EXCEEDED,
    /** IBL-E6 / ADR-0067: nested contentModuleRef missing from template pins. */
    CONTENT_MODULE_NESTING_UNPINNED,
    /** IBL-E2 / ADR-0063: inclusion rule referenceKey not declared on the version. */
    COMPOSITION_INCLUSION_REFERENCE_INVALID,
    UNSUPPORTED_STRUCTURED_NODES,
    PASTE_CLEANING_BLOCKERS,
    /** ADR-0042: |pdfPages - authorWordPageCount| exceeds 2× pagination delta budget. */
    PAGINATION_DELTA_BUDGET,
    BLOCKER_STATUS,
    FIDELITY_WARNINGS_VIEWED
}
