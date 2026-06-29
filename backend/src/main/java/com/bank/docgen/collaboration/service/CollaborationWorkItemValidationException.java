package com.bank.docgen.collaboration.service;

public class CollaborationWorkItemValidationException extends RuntimeException {

    private final String messageKey;

    public CollaborationWorkItemValidationException(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
