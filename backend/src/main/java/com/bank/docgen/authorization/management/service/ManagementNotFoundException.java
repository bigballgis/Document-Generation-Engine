package com.bank.docgen.authorization.management.service;

/**
 * Base type for management-plane NOT_FOUND failures. These are intentionally
 * generic and never disclose whether an out-of-scope resource exists.
 */
public abstract class ManagementNotFoundException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    protected ManagementNotFoundException(String errorCode, String messageKey) {
        super(errorCode);
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
