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

    /** Same key and hash still in flight; caller must retry without re-running generation. */
    public static final String REQUEST_IN_PROGRESS = "REQUEST_IN_PROGRESS";

    private final String idempotencyKey;
    private final String conflictType;

    public IdempotencyConflictException(String idempotencyKey) {
        this(idempotencyKey, REQUEST_SEMANTICS_MISMATCH);
    }

    public static IdempotencyConflictException requestInProgress(String idempotencyKey) {
        return new IdempotencyConflictException(idempotencyKey, REQUEST_IN_PROGRESS);
    }

    private IdempotencyConflictException(String idempotencyKey, String conflictType) {
        super("Idempotency conflict for key " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
        this.conflictType = conflictType;
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
