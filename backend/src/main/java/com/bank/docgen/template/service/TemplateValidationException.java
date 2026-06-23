package com.bank.docgen.template.service;

public class TemplateValidationException extends RuntimeException {

    private final String messageKey;

    public TemplateValidationException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
