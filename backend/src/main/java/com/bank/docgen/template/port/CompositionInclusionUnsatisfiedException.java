package com.bank.docgen.template.port;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

/**
 * ADR-0063 / IBL-E2 — required inclusion rule matched none of the request axes.
 */
public class CompositionInclusionUnsatisfiedException extends RuntimeException {

    private final String messageKey;

    public CompositionInclusionUnsatisfiedException() {
        super("api.error.template.compositionInclusionUnsatisfied");
        this.messageKey = "api.error.template.compositionInclusionUnsatisfied";
    }

    public String errorCode() {
        return ApiErrorCodes.COMPOSITION_INCLUSION_UNSATISFIED;
    }

    public String messageKey() {
        return messageKey;
    }
}
