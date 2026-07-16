package com.bank.docgen.legalhold.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class LegalHoldNotFoundException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public LegalHoldNotFoundException() {
        super(ApiErrorCodes.LEGAL_HOLD_NOT_FOUND);
        this.errorCode = ApiErrorCodes.LEGAL_HOLD_NOT_FOUND;
        this.messageKey = "api.error.notFound.legalHoldNotFound";
    }

    public String errorCode() {
        return errorCode;
    }

    public String messageKey() {
        return messageKey;
    }
}
