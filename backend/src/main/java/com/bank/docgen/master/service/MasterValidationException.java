package com.bank.docgen.master.service;

public class MasterValidationException extends RuntimeException {

    private final String messageKey;

    public MasterValidationException(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
