package com.bank.docgen.authorization.management.domain;

public enum ManagementRole {
    GLOBAL_ADMIN,
    GROUP_ADMIN,
    MASTER_DESIGNER,
    TEMPLATE_AUTHOR,
    TEMPLATE_TESTER,
    TEMPLATE_APPROVER,
    AUDIT_ADMIN;

    public static ManagementRole fromCode(String code) {
        return ManagementRole.valueOf(code);
    }
}
