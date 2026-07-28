package com.bank.docgen.runtime.service;

import com.bank.docgen.sharedkernel.api.FieldError;
import java.util.List;

/**
 * FOS-W11-4/W11-5: request-shape failures that must surface as VALIDATION + fieldErrors
 * ({@code REQUEST_BODY_INVALID}), not template validation.
 */
public final class GenerateRequestShapeException extends RuntimeException {

    public static final String MESSAGE_KEY = "api.error.validation.requestBodyInvalid";

    private final List<FieldError> fieldErrors;

    public GenerateRequestShapeException(List<FieldError> fieldErrors) {
        super(MESSAGE_KEY);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public String messageKey() {
        return MESSAGE_KEY;
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
