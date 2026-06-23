package com.bank.docgen.runtime.service;

/** Idempotency retention and response header values per ADR 0004 and OpenAPI v1. */
public final class IdempotencyConstants {

    /** Seven-day retention window (seconds). */
    public static final long RETENTION_SECONDS = 7L * 24 * 60 * 60;

    public static final String STATUS_NEW = "IDEMPOTENCY_NEW";
    public static final String STATUS_REPLAYED = "IDEMPOTENCY_REPLAYED";

    private IdempotencyConstants() {
    }
}
