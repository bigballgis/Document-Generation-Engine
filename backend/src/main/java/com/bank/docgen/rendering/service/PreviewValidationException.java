package com.bank.docgen.rendering.service;

public class PreviewValidationException extends RuntimeException {

    private final String messageKey;

    public PreviewValidationException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
