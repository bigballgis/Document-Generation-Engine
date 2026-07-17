package com.bank.docgen.library.service;

public class LibraryExportValidationException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public LibraryExportValidationException(String errorCode, String messageKey) {
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
