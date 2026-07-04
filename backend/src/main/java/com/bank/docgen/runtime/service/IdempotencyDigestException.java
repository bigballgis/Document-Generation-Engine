package com.bank.docgen.runtime.service;

/**
 * LR-B7 (absorbs OPT-E9): raised when the idempotency request digest cannot be computed.
 * This is a hard, retryable 500-class failure — the service must never weaken the
 * idempotency key by falling back to the raw payload (which would leak variable values
 * into the idempotency store and change conflict semantics silently).
 */
public class IdempotencyDigestException extends RuntimeException {

    public IdempotencyDigestException(Throwable cause) {
        super("Failed to compute idempotency request digest", cause);
    }

    public String messageKey() {
        return "api.error.generation.idempotencyDigestFailed";
    }
}
