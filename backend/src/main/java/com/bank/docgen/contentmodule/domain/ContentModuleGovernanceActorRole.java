package com.bank.docgen.contentmodule.domain;

public enum ContentModuleGovernanceActorRole {
    GLOBAL_ADMIN,
    GROUP_ADMIN,
    /** Former TEMPLATE_APPROVER decide path — session must be GROUP_ADMIN (ADR-0070 absorb). */
    APPROVER,
    /** Former TEMPLATE_AUTHOR ∪ MASTER_DESIGNER author path. */
    DOCUMENT_AUTHOR
}
