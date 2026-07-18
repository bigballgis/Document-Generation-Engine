package com.bank.docgen.infrastructure.resilience;

/**
 * Resilience / mapped generation timeout — HTTP 504 {@code GENERATION_TIMEOUT}.
 * Message text is resolved via {@link #messageKey()}; no timeout class names in the payload.
 */
public class GenerationTimeoutException extends RuntimeException {

    public GenerationTimeoutException() {
        super();
    }

    public String messageKey() {
        return "api.error.generation.generationTimeout";
    }
}
