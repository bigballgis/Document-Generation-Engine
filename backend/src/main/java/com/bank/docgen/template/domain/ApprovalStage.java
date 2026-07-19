package com.bank.docgen.template.domain;

/**
 * IBL-E3 / ADR-0064 — multi-stage approval stage under {@link ApprovalMatrixMode#LEGAL_THEN_COMPLIANCE}.
 * Null/absent for {@link ApprovalMatrixMode#SINGLE_TRACK}.
 */
public enum ApprovalStage {
    LEGAL,
    COMPLIANCE;

    /**
     * Derive the unique stage from an approval sub-state, or {@code null} when not in a
     * multi-stage decision window.
     */
    public static ApprovalStage fromSubState(ApprovalSubState subState) {
        if (subState == ApprovalSubState.PENDING_LEGAL_DECISION) {
            return LEGAL;
        }
        if (subState == ApprovalSubState.PENDING_COMPLIANCE_DECISION) {
            return COMPLIANCE;
        }
        return null;
    }
}
