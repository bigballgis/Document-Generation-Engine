package com.bank.docgen.runtime.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

/**
 * IBL-E1 — runtime {@code context.locale} incompatible with pinned template locale.
 */
public class TemplateLocaleMismatchException extends RuntimeException {

    private final String messageKey;

    public TemplateLocaleMismatchException() {
        super("api.error.runtime.templateLocaleMismatch");
        this.messageKey = "api.error.runtime.templateLocaleMismatch";
    }

    public String errorCode() {
        return ApiErrorCodes.TEMPLATE_LOCALE_MISMATCH;
    }

    public String messageKey() {
        return messageKey;
    }
}
