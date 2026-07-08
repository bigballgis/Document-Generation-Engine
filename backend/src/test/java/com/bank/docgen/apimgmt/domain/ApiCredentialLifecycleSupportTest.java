package com.bank.docgen.apimgmt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiCredentialLifecycleSupportTest {

    @Test
    void resolveEffectiveStatus_marksCredentialExpiringWithinThirtyDays() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credentialCreatedAt(
                now.minus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS - 10, ChronoUnit.DAYS)
        );

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.EXPIRING_SOON);
        assertThat(ApiCredentialLifecycleSupport.isExpiringCredential(credential, now)).isTrue();
    }

    @Test
    void resolveEffectiveStatus_marksCredentialExpiredAfterLifetime() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credentialCreatedAt(
                now.minus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS + 1, ChronoUnit.DAYS)
        );

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.EXPIRED);
        assertThat(ApiCredentialLifecycleSupport.isActiveCredential(credential, now)).isFalse();
    }

    @Test
    void resolveEffectiveStatus_preservesRevokedStatus() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        ApiCredentialEntity credential = credentialCreatedAt(now.minus(10, ChronoUnit.DAYS));
        credential.revoke();

        assertThat(ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now))
                .isEqualTo(ApiCredentialStatus.REVOKED);
    }

    private static ApiCredentialEntity credentialCreatedAt(Instant createdAt) {
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(),
                "CRED-TEST01",
                UUID.randomUUID(),
                "hash",
                "10000001"
        );
        try {
            var field = ApiCredentialEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(credential, createdAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return credential;
    }
}
