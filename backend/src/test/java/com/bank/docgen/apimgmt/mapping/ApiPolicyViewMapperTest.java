package com.bank.docgen.apimgmt.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiPolicyViewMapperTest {

    private ApiPolicyViewMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = ApiPolicyViewMapperFactory.create(new ObjectMapper());
    }

    @Test
    void toPolicyView_mapsEntityFieldsAndJsonLists() {
        UUID templateId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-06-25T12:00:00Z");
        ApiPolicyEntity policy = new ApiPolicyEntity(templateId, templateId, "[\"G1\",\"G2\"]", "actor01");
        policy.replaceConfiguration(
                "[\"G1\",\"G2\"]",
                "v2.0.0",
                "[\"DOCX\",\"PDF\"]",
                "[\"SYNC_STREAM\",\"ASYNC\"]",
                true,
                50,
                true,
                false,
                "actor01"
        );

        ApiPolicyView view = mapper.toPolicyView(policy);

        assertThat(view.templateId()).isEqualTo(templateId.toString());
        assertThat(view.policyVersion()).isEqualTo(policy.getPolicyVersion());
        assertThat(view.allowedAdGroups()).containsExactly("G1", "G2");
        assertThat(view.defaultRouteReleaseVersion()).isEqualTo("v2.0.0");
        assertThat(view.outputFormats()).containsExactly("DOCX", "PDF");
        assertThat(view.outputModes()).containsExactly("SYNC_STREAM", "ASYNC");
        assertThat(view.batchEnabled()).isTrue();
        assertThat(view.maxBatchSize()).isEqualTo(50);
        assertThat(view.batchSyncMaxItems()).isEqualTo(50);
        assertThat(view.batchAsyncMaxItems()).isEqualTo(10000);
        assertThat(view.docxEncryptionEnabled()).isTrue();
        assertThat(view.pdfEncryptionEnabled()).isFalse();
        assertThat(view.saveGeneratedDocuments()).isTrue();
        assertThat(view.invocationRecordRetentionDays()).isEqualTo(90);
        assertThat(view.documentRetentionDays()).isEqualTo(30);
        assertThat(view.updatedAt()).isEqualTo(policy.getUpdatedAt());
    }

    @Test
    void toPolicyView_returnsEmptyListForInvalidAllowedAdGroupsJson() {
        UUID templateId = UUID.randomUUID();
        ApiPolicyEntity policy = new ApiPolicyEntity(templateId, templateId, "not-json", "actor01");

        ApiPolicyView view = mapper.toPolicyView(policy);

        assertThat(view.allowedAdGroups()).isEmpty();
        assertThat(view.outputFormats()).containsExactly("DOCX", "PDF");
        assertThat(view.outputModes()).containsExactly("SYNC_STREAM", "SYNC_DOWNLOAD_URL", "ASYNC_TASK");
    }

    @Test
    void toCredentialSummary_mapsCredentialFields() {
        UUID credentialId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        ApiCredentialEntity credential = new ApiCredentialEntity(
                credentialId,
                "ext-cred-1",
                templateId,
                "hash",
                "actor01"
        );
        credential.revoke();

        ApiCredentialSummaryView view = mapper.toCredentialSummary(credential);

        assertThat(view.credentialId()).isEqualTo(credentialId.toString());
        assertThat(view.externalId()).isEqualTo("ext-cred-1");
        assertThat(view.status()).isEqualTo(ApiCredentialStatus.REVOKED.name());
        assertThat(view.createdAt()).isEqualTo(credential.getCreatedAt());
        assertThat(view.revokedAt()).isEqualTo(credential.getRevokedAt());
        assertThat(view.expiresAt()).isEqualTo(credential.getExpiresAt());
    }

    @Test
    void toRuntimeCredentialSummary_buildsFingerprintPrefixAndExpiresAt() {
        UUID templateId = UUID.randomUUID();
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(),
                "runtime-ext",
                templateId,
                "hash",
                "actor01"
        );

        RuntimeCredentialSummaryView view = mapper.toRuntimeCredentialSummary(credential);

        assertThat(view.credentialExternalId()).isEqualTo("runtime-ext");
        assertThat(view.status()).isEqualTo(ApiCredentialStatus.ACTIVE.name());
        assertThat(view.fingerprintSummary()).isEqualTo("fp-runtime-ext");
        assertThat(view.expiresAt()).isEqualTo(credential.getExpiresAt());
    }

    @Test
    void toRuntimeCredentialSummary_usesEffectiveExpiringSoonStatus() {
        Instant now = Instant.now();
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(),
                "runtime-soon",
                UUID.randomUUID(),
                "hash",
                "actor01"
        );
        try {
            var field = ApiCredentialEntity.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(credential, now.plus(5, java.time.temporal.ChronoUnit.DAYS));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        RuntimeCredentialSummaryView view = mapper.toRuntimeCredentialSummary(credential);

        assertThat(view.status()).isEqualTo(ApiCredentialStatus.EXPIRING_SOON.name());
        assertThat(view.expiresAt()).isEqualTo(credential.getExpiresAt());
    }
}
