package com.bank.docgen.legalhold.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class LegalHoldAlreadyReleasedException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public LegalHoldAlreadyReleasedException() {
        super(ApiErrorCodes.LEGAL_HOLD_ALREADY_RELEASED);
        this.errorCode = ApiErrorCodes.LEGAL_HOLD_ALREADY_RELEASED;
        this.messageKey = "api.error.conflict.legalHoldAlreadyReleased";
    }

    public String errorCode() {
        return errorCode;
    }

    public String messageKey() {
        return messageKey;
    }
}
