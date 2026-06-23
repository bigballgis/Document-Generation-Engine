package com.bank.docgen.runtime.service;

/**
 * Raised when a still-live idempotency record exists for the same
 * (idempotencyKey, templateId) but the request semantics differ (different
 * request hash), or when a concurrent insert collides with a different request.
 *
 * <p>Per ADR 0004 this is an idempotency conflict and must surface as a
 * deterministic 409 response, never a 500 from a unique-constraint violation.
 */
public class IdempotencyConflictException extends RuntimeException {

    /** Baseline conflict type per ADR 0004 (safe summary value). */
    public static final String REQUEST_SEMANTICS_MISMATCH = "REQUEST_SEMANTICS_MISMATCH";

    private final String idempotencyKey;
    private final String conflictType;

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency conflict for key " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
        this.conflictType = REQUEST_SEMANTICS_MISMATCH;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String conflictType() {
        return conflictType;
    }

    public String messageKey() {
        return "api.error.runtime.idempotencyConflict";
    }
}
