package com.bank.docgen.collaboration.domain;

public enum CollaborationWorkItemTriggerType {
    SUBMIT_FOR_TEST,
    TEST_FAILURE_OR_RETURN_TO_DRAFT,
    SUBMIT_FOR_APPROVAL,
    APPROVAL_FAILURE_OR_RETURN_TO_DRAFT,
    APPROVAL_PENDING_RELEASE,
    TIMEOUT_ESCALATION
}
