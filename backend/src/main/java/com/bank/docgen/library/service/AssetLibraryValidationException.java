package com.bank.docgen.library.service;

public class AssetLibraryValidationException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public AssetLibraryValidationException(String errorCode, String messageKey) {
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
