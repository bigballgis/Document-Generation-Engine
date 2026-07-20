package com.bank.docgen.template.api;

/**
 * Per-template-version outcome for IBL-E5 bulk re-pin.
 */
public enum BulkRepinItemStatus {
    WOULD_APPLY,
    APPLIED,
    SKIPPED_LOCKED,
    SKIPPED_ALREADY_AT_TARGET,
    SKIPPED_NO_MATCH,
    FAILED
}
