package com.bank.docgen.authorization.management.domain;

public enum ManagementRole {
    GLOBAL_ADMIN,
    GROUP_ADMIN,
    TEMPLATE_AUTHOR,
    AUDIT_ADMIN;

    public static ManagementRole fromCode(String code) {
        return ManagementRole.valueOf(code);
    }
}
