package com.bank.docgen.apimgmt.service;

import org.springframework.http.HttpStatus;

/**
 * Fail-closed CE-G06 regenerate errors with stable envelope codes.
 */
public class InvocationRegenerationException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String category;
    private final String messageKey;

    public InvocationRegenerationException(
            HttpStatus httpStatus,
            String errorCode,
            String category,
            String messageKey
    ) {
        super(messageKey);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.category = category;
        this.messageKey = messageKey;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String errorCode() {
        return errorCode;
    }

    public String category() {
        return category;
    }

    public String messageKey() {
        return messageKey;
    }
}
