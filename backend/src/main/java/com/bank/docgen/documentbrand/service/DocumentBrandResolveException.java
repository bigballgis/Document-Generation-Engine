package com.bank.docgen.documentbrand.service;

/**
 * Fail-closed document-brand / legal-entity resolution (ADR-0065).
 */
public class DocumentBrandResolveException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public DocumentBrandResolveException(String errorCode, String messageKey) {
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
