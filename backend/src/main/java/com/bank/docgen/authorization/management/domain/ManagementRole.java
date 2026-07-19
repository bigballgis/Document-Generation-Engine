package com.bank.docgen.authorization.management.domain;

public enum ManagementRole {
    GLOBAL_ADMIN,
    GROUP_ADMIN,
    MASTER_DESIGNER,
    TEMPLATE_AUTHOR,
    TEMPLATE_TESTER,
    TEMPLATE_APPROVER,
    /** IBL-E3 / ADR-0064 — forced LEGAL-stage reviewer under LEGAL_THEN_COMPLIANCE. */
    LEGAL_REVIEWER,
    AUDIT_ADMIN;

    public static ManagementRole fromCode(String code) {
        return ManagementRole.valueOf(code);
    }
}
