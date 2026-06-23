package com.bank.docgen.authorization.management.service;

/**
 * Base type for management-plane fail-closed authorization denials (HTTP 403).
 * Subclasses carry the stable escalation error code and English messageKey.
 */
public abstract class ManagementForbiddenException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    protected ManagementForbiddenException(String errorCode, String messageKey) {
        super(errorCode);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
    }

    public String errorCode() {
        return errorCode;
    }

    public String messageKey() {
        return messageKey;
    }
}
