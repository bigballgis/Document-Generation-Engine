package com.bank.docgen.apimgmt.domain;

import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Credential expiry and effective-status resolution from persisted {@code api_credential.expires_at}
 * (ADR-0009). Rotation grace is {@link #ROTATION_GRACE_DAYS} days (FOS D11 / 2026-07-26 amendment).
 */
public final class ApiCredentialLifecycleSupport {

    public static final int DEFAULT_EXPIRY_DAYS = 180;
    public static final int MAX_EXPIRY_DAYS = 365;
    public static final int EXPIRING_SOON_WINDOW_DAYS = 30;
    /** ADR-0009 amended 2026-07-26 — previous secret accepted for this many days after rotate. */
    public static final int ROTATION_GRACE_DAYS = 28;
    public static final int EXPIRY_ALERT_SEVEN_DAY_THRESHOLD = 7;
    public static final int EXPIRY_ALERT_ONE_DAY_THRESHOLD = 1;

    private ApiCredentialLifecycleSupport() {
    }

    public static Instant defaultExpiresAt(Instant from) {
        return from.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
    }

    public static Instant expiresAtForDays(Instant from, int expiryDays) {
        if (expiryDays < 1 || expiryDays > MAX_EXPIRY_DAYS) {
            throw new IllegalArgumentException("expiryDays must be between 1 and " + MAX_EXPIRY_DAYS);
        }
        return from.plus(expiryDays, ChronoUnit.DAYS);
    }

    public static Instant rotationGracePeriodEndsAt(Instant rotatedAt) {
        return rotatedAt.plus(ROTATION_GRACE_DAYS, ChronoUnit.DAYS);
    }

    public static Instant resolveExpiresAt(ApiCredentialEntity credential) {
        Instant expiresAt = credential.getExpiresAt();
        if (expiresAt == null) {
            throw new IllegalStateException("api_credential.expires_at is required");
        }
        return expiresAt;
    }

    public static ApiCredentialStatus resolveEffectiveStatus(ApiCredentialEntity credential, Instant now) {
        Objects.requireNonNull(now, "now");
        if (credential.getStatus() == ApiCredentialStatus.REVOKED) {
            return ApiCredentialStatus.REVOKED;
        }
        if (credential.getStatus() == ApiCredentialStatus.EXPIRED) {
            return ApiCredentialStatus.EXPIRED;
        }
        Instant expiresAt = resolveExpiresAt(credential);
        if (!expiresAt.isAfter(now)) {
            return ApiCredentialStatus.EXPIRED;
        }
        if (!expiresAt.isAfter(now.plus(EXPIRING_SOON_WINDOW_DAYS, ChronoUnit.DAYS))) {
            return ApiCredentialStatus.EXPIRING_SOON;
        }
        if (credential.getStatus() == ApiCredentialStatus.EXPIRING_SOON) {
            return ApiCredentialStatus.EXPIRING_SOON;
        }
        return ApiCredentialStatus.ACTIVE;
    }

    public static boolean isExpiringCredential(ApiCredentialEntity credential, Instant now) {
        ApiCredentialStatus effective = resolveEffectiveStatus(credential, now);
        return effective == ApiCredentialStatus.EXPIRING_SOON;
    }

    public static boolean isActiveCredential(ApiCredentialEntity credential, Instant now) {
        ApiCredentialStatus effective = resolveEffectiveStatus(credential, now);
        return effective == ApiCredentialStatus.ACTIVE || effective == ApiCredentialStatus.EXPIRING_SOON;
    }

    /** Rotate allowed only when effective status is still callable (ACTIVE / EXPIRING_SOON). */
    public static boolean isRotatable(ApiCredentialEntity credential, Instant now) {
        return isActiveCredential(credential, now);
    }

    /**
     * Whether the previous secret hash is still accepted at {@code now}
     * (FOS-W10-1 / ADR-0009 28-day grace).
     */
    public static boolean isPreviousSecretWithinGrace(ApiCredentialEntity credential, Instant now) {
        Objects.requireNonNull(now, "now");
        String previous = credential.getPreviousSecretHash();
        Instant endsAt = credential.getRotationGracePeriodEndsAt();
        if (previous == null || previous.isBlank() || endsAt == null) {
            return false;
        }
        return now.isBefore(endsAt);
    }

    /**
     * Alert severity for an expiring credential: INFO in the 30-day window; WARNING at ≤7 days
     * (includes the 1-day threshold). OpenAPI {@code ApiAccessAlertSeverity} has no CRITICAL.
     */
    public static ApiAccessAlertSeverity expiryAlertSeverity(Instant expiresAt, Instant now) {
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(now, "now");
        long daysRemaining = ChronoUnit.DAYS.between(now, expiresAt);
        if (daysRemaining <= EXPIRY_ALERT_SEVEN_DAY_THRESHOLD) {
            return ApiAccessAlertSeverity.WARNING;
        }
        return ApiAccessAlertSeverity.INFO;
    }
}
