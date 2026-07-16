package com.bank.docgen.legalhold.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class LegalHoldValidationException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public LegalHoldValidationException(String messageKey) {
        this(ApiErrorCodes.REQUEST_BODY_INVALID, messageKey);
    }

    public LegalHoldValidationException(String errorCode, String messageKey) {
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
