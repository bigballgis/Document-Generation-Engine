package com.bank.docgen.runtime.domain;

/**
 * Unified error envelope snapshot persisted on failed invocation records (CE-U11).
 */
public record InvocationErrorEnvelope(
        String code,
        String category,
        String messageKey,
        boolean retryable,
        String message
) {
}
