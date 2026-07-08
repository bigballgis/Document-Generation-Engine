package com.bank.docgen.apimgmt.domain;

import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Transitional credential expiry derivation until {@code api_credential.expires_at} is persisted (ADR-0009).
 */
public final class ApiCredentialLifecycleSupport {

    public static final int DEFAULT_EXPIRY_DAYS = 180;
    public static final int EXPIRING_SOON_WINDOW_DAYS = 30;

    private ApiCredentialLifecycleSupport() {
    }

    public static Instant resolveExpiresAt(ApiCredentialEntity credential) {
        return credential.getCreatedAt().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
    }

    public static ApiCredentialStatus resolveEffectiveStatus(ApiCredentialEntity credential, Instant now) {
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
}
