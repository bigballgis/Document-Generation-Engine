package com.bank.docgen.collaboration.domain;

public enum CollaborationWorkItemQueue {
    TEST,
    APPROVAL,
    /** IBL-E3 — LEGAL-stage queue under LEGAL_THEN_COMPLIANCE (distinct from APPROVAL/COMPLIANCE). */
    LEGAL,
    REMEDIATION,
    PENDING_RELEASE,
    ESCALATION
}
