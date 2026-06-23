package com.bank.docgen.audit.domain;

public enum AuditReadActorRole {
    AUDIT_ADMIN,
    GLOBAL_ADMIN,
    GROUP_ADMIN;

    public static AuditReadActorRole fromCode(String code) {
        return AuditReadActorRole.valueOf(code);
    }
}
