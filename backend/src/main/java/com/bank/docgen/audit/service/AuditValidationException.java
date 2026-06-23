package com.bank.docgen.audit.service;

public class AuditValidationException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public AuditValidationException(String errorCode, String messageKey) {
        super(messageKey);
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
