package com.bank.docgen.runtime.security;

/**
 * Outcome of a runtime rate-limit consume attempt.
 */
public final class RateLimitDecision {

    public enum Status {
        ALLOWED,
        DENIED,
        BACKEND_UNAVAILABLE
    }

    private final Status status;
    private final long nanosToWaitForRefill;

    private RateLimitDecision(Status status, long nanosToWaitForRefill) {
        this.status = status;
        this.nanosToWaitForRefill = nanosToWaitForRefill;
    }

    public static RateLimitDecision allowed() {
        return new RateLimitDecision(Status.ALLOWED, 0L);
    }

    public static RateLimitDecision denied(long nanosToWaitForRefill) {
        return new RateLimitDecision(Status.DENIED, Math.max(0L, nanosToWaitForRefill));
    }

    public static RateLimitDecision backendUnavailable() {
        return new RateLimitDecision(Status.BACKEND_UNAVAILABLE, 0L);
    }

    public Status status() {
        return status;
    }

    public boolean isAllowed() {
        return status == Status.ALLOWED;
    }

    public boolean isDenied() {
        return status == Status.DENIED;
    }

    public boolean isBackendUnavailable() {
        return status == Status.BACKEND_UNAVAILABLE;
    }

    public long nanosToWaitForRefill() {
        return nanosToWaitForRefill;
    }
}
