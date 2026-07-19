package com.bank.docgen.template.port;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

/**
 * ADR-0063 / IBL-E2 — INCLUDE'd CM version jurisdiction differs from context.jurisdiction.
 */
public class ContentModuleJurisdictionMismatchException extends RuntimeException {

    private final String messageKey;

    public ContentModuleJurisdictionMismatchException() {
        super("api.error.template.contentModuleJurisdictionMismatch");
        this.messageKey = "api.error.template.contentModuleJurisdictionMismatch";
    }

    public String errorCode() {
        return ApiErrorCodes.CONTENT_MODULE_JURISDICTION_MISMATCH;
    }

    public String messageKey() {
        return messageKey;
    }
}
