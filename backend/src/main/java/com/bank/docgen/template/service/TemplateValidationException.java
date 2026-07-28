package com.bank.docgen.template.service;

public class TemplateValidationException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public TemplateValidationException(String messageKey) {
        this(messageKey, new Object[0]);
    }

    public TemplateValidationException(String messageKey, Object... messageArgs) {
        super(messageKey);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
    }

    public String messageKey() {
        return messageKey;
    }

    public Object[] messageArgs() {
        return messageArgs.clone();
    }
}
