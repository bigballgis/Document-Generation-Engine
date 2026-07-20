package com.bank.docgen.documentbrand.service;

public class DocumentBrandCatalogException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public DocumentBrandCatalogException(String errorCode, String messageKey) {
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
