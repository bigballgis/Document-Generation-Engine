package com.bank.docgen.apimgmt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiCredentialLifecycleSupportTest {

    @Test
    void rotationGracePeriod_isTwentyEightDaysPerAdr0009Amendment() {
        Instant rotatedAt = Instant.parse("2026-07-08T00:00:00Z");
        assertThat(ApiCredentialLifecycleSupport.ROTATION_GRACE_DAYS).isEqualTo(28);
        assertThat(ApiCredentialLifecycleSupport.rotationGracePeriodEndsAt(rotatedAt))
                .isEqualTo(rotatedAt.plus(28, ChronoUnit.DAYS));
        assertThat(ApiCredentialLifecycleSupport.expiryAlertSeverity(
                rotatedAt.plus(25, ChronoUnit.DAYS), rotatedAt))
                .isEqualTo(ApiAccessAlertSeverity.INFO);
        assertThat(ApiCredentialLifecycleSupport.expiryAlertSeverity(
                rotatedAt.plus(5, ChronoUnit.DAYS), rotatedAt))
                .isEqualTo(ApiAccessAlertSeverity.WARNING);
    }

    @Test
    void resolveExpiresAt_usesPersistedColumnNotCreatedAtDerivation() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        Instant earlyCreated = now.minus(400, ChronoUnit.DAYS);
        Instant explicitExpiry = now.plus(90, ChronoUnit.DAYS);
        ApiCredentialEntity credential = credential(earlyCreated, explicitExpiry);

        assertThat(ApiCredentialLifecycleSupport.resolveExpiresAt(credential)).isEqualTo(explicitExpiry);
        assertThat(ApiCredentialLifecycleSupport.resolveExpiresAt(credential))
                .isNotEqualTo(earlyCreated.plus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
    }

    @Test
    void resolveEffectiveStatus_marksCredentialExpiringWithinThirtyDays() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credential(
                now.minus(10, ChronoUnit.DAYS),
                now.plus(10, ChronoUnit.DAYS)
        );

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.EXPIRING_SOON);
        assertThat(ApiCredentialLifecycleSupport.isExpiringCredential(credential, now)).isTrue();
        assertThat(ApiCredentialLifecycleSupport.isActiveCredential(credential, now)).isTrue();
    }

    @Test
    void resolveEffectiveStatus_marksCredentialExpiredAfterExpiry() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credential(
                now.minus(200, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.SECONDS)
        );

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.EXPIRED);
        assertThat(ApiCredentialLifecycleSupport.isActiveCredential(credential, now)).isFalse();
    }

    @Test
    void resolveEffectiveStatus_treatsExactNowAsExpired() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credential(now.minus(10, ChronoUnit.DAYS), now);

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.EXPIRED);
    }

    @Test
    void resolveEffectiveStatus_treatsExactWindowBoundaryAsExpiringSoon() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        Instant windowEnd = now.plus(ApiCredentialLifecycleSupport.EXPIRING_SOON_WINDOW_DAYS, ChronoUnit.DAYS);
        ApiCredentialEntity credential = credential(now.minus(10, ChronoUnit.DAYS), windowEnd);

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.EXPIRING_SOON);
    }

    @Test
    void resolveEffectiveStatus_preservesRevokedStatus() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credential(
                now.minus(10, ChronoUnit.DAYS),
                now.plus(100, ChronoUnit.DAYS)
        );
        credential.revoke();

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.REVOKED);
    }

    @Test
    void resolveEffectiveStatus_preservesPersistedExpiredEvenIfColumnIsFuture() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credential(
                now.minus(10, ChronoUnit.DAYS),
                now.plus(100, ChronoUnit.DAYS)
        );
        setStatus(credential, ApiCredentialStatus.EXPIRED);

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.EXPIRED);
    }

    @Test
    void resolveEffectiveStatus_returnsActiveWhenFarFromExpiry() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credential(
                now.minus(10, ChronoUnit.DAYS),
                now.plus(100, ChronoUnit.DAYS)
        );

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.ACTIVE);
        assertThat(ApiCredentialLifecycleSupport.isActiveCredential(credential, now)).isTrue();
    }

    @Test
    void defaultExpiresAt_isCreatedAtPlusOneHundredEightyDays() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(ApiCredentialLifecycleSupport.defaultExpiresAt(createdAt))
                .isEqualTo(createdAt.plus(180, ChronoUnit.DAYS));
    }

    private static ApiCredentialEntity credential(Instant createdAt, Instant expiresAt) {
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(),
                "CRED-TEST01",
                UUID.randomUUID(),
                "hash",
                "10000001"
        );
        setInstant(credential, "createdAt", createdAt);
        setInstant(credential, "expiresAt", expiresAt);
        return credential;
    }

    private static void setStatus(ApiCredentialEntity credential, ApiCredentialStatus status) {
        try {
            var field = ApiCredentialEntity.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(credential, status);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void setInstant(ApiCredentialEntity credential, String fieldName, Instant value) {
        try {
            var field = ApiCredentialEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(credential, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
