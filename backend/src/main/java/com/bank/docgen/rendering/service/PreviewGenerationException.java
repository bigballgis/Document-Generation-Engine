package com.bank.docgen.rendering.service;

public class PreviewGenerationException extends RuntimeException {

    private final String messageKey;

    public PreviewGenerationException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
