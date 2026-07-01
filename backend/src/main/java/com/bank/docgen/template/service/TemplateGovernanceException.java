package com.bank.docgen.template.service;

import org.springframework.http.HttpStatus;

public class TemplateGovernanceException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;
    private final HttpStatus httpStatus;

    public TemplateGovernanceException(String errorCode, String messageKey, HttpStatus httpStatus) {
        super(messageKey);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
    }

    public String errorCode() {
        return errorCode;
    }

    public String messageKey() {
        return messageKey;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
