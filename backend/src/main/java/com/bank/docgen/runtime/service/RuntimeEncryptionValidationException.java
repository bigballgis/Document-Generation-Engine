package com.bank.docgen.runtime.service;

public class RuntimeEncryptionValidationException extends RuntimeException {

    private final String messageKey;

    public RuntimeEncryptionValidationException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
