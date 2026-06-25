package com.bank.docgen.template.service;

public class TestDataSetImmutableException extends RuntimeException {

    private final String messageKey;

    public TestDataSetImmutableException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
