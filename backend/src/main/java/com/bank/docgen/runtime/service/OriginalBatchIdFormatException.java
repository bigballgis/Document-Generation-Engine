package com.bank.docgen.runtime.service;

import com.bank.docgen.sharedkernel.api.FieldError;
import java.util.List;

/**
 * Invalid originalBatchId pattern / empty string → 400 REQUEST_BODY_INVALID (CE-C05).
 */
public class OriginalBatchIdFormatException extends RuntimeException {

    private final List<FieldError> fieldErrors;

    public OriginalBatchIdFormatException() {
        super("api.error.validation.requestBodyInvalid");
        this.fieldErrors = List.of(
                new FieldError("originalBatchId", "PATTERN_MISMATCH", "originalBatchId must match BATCH-* pattern")
        );
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }

    public String messageKey() {
        return "api.error.validation.requestBodyInvalid";
    }
}
