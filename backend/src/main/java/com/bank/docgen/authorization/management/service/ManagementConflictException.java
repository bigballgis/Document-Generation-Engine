package com.bank.docgen.authorization.management.service;

/**
 * Base type for management-plane CONFLICT failures (duplicate unique keys).
 */
public abstract class ManagementConflictException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    protected ManagementConflictException(String errorCode, String messageKey) {
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
