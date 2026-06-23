package com.bank.docgen.runtime.service;

public class RuntimeBatchValidationException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public RuntimeBatchValidationException(String errorCode, String messageKey) {
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
