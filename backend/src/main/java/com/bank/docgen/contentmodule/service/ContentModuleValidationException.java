package com.bank.docgen.contentmodule.service;

public class ContentModuleValidationException extends RuntimeException {

    private final String messageKey;

    public ContentModuleValidationException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
