package com.bank.docgen.runtime.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

/**
 * Opaque 404 when originalBatchId does not resolve to a same-credential BATCH_ROOT (CE-C05).
 */
public class OriginalBatchNotFoundException extends RuntimeException {

    public OriginalBatchNotFoundException() {
        super("api.error.batch.originalBatchNotFound");
    }

    public String errorCode() {
        return ApiErrorCodes.ORIGINAL_BATCH_NOT_FOUND;
    }

    public String messageKey() {
        return "api.error.batch.originalBatchNotFound";
    }
}
