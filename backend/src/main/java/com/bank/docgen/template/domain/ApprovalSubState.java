package com.bank.docgen.template.domain;

public enum ApprovalSubState {
    PENDING_SUBMIT,
    PENDING_DECISION,
    /** IBL-E3 — awaiting LEGAL stage under {@link ApprovalMatrixMode#LEGAL_THEN_COMPLIANCE}. */
    PENDING_LEGAL_DECISION,
    /** IBL-E3 — awaiting COMPLIANCE stage under {@link ApprovalMatrixMode#LEGAL_THEN_COMPLIANCE}. */
    PENDING_COMPLIANCE_DECISION
}
