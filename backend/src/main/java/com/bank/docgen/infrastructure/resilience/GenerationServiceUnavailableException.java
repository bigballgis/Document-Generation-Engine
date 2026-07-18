package com.bank.docgen.infrastructure.resilience;

/**
 * Circuit open / bulkhead full / unknown resilience failure — HTTP 503
 * {@code GENERATION_SERVICE_UNAVAILABLE}. Message text is resolved via {@link #messageKey()};
 * this exception carries no Resilience4j class names or breaker identifiers.
 */
public class GenerationServiceUnavailableException extends RuntimeException {

    public GenerationServiceUnavailableException() {
        super();
    }

    public String messageKey() {
        return "api.error.generation.generationServiceUnavailable";
    }
}
