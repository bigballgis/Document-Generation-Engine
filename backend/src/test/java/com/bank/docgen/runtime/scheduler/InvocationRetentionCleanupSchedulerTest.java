package com.bank.docgen.runtime.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvocationRetentionCleanupSchedulerTest {

    @Mock
    private ApiInvocationRecordRepository repository;
    @Mock
    private ObjectStoragePort objectStoragePort;

    private InvocationRetentionCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new InvocationRetentionCleanupScheduler(repository, objectStoragePort);
    }

    @Test
    void cleanExpiredRecords_deletesPastRecordExpiryRows() {
        ApiInvocationRecordEntity expired = sampleRecord("INV-EXPIRED1", "storage/doc.docx");
        expired = expiredWithRecordExpiry(expired, Instant.now().minusSeconds(60));
        when(repository.findByRecordExpiresAtBefore(any(Instant.class))).thenReturn(List.of(expired));

        scheduler.cleanExpiredRecords();

        verify(repository).deleteAll(List.of(expired));
        verify(objectStoragePort, never()).delete(anyString());
    }

    @Test
    void cleanExpiredDocumentArtifacts_deletesStorageAndClearsKey() {
        ApiInvocationRecordEntity artifactExpired = artifactExpiredRecord(
                "INV-DOCEXP1",
                "generated/doc-1.docx",
                Instant.now().minusSeconds(60)
        );
        when(repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(any(Instant.class)))
                .thenReturn(List.of(artifactExpired));
        when(objectStoragePort.exists("generated/doc-1.docx")).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.cleanExpiredDocumentArtifacts();

        verify(objectStoragePort).delete("generated/doc-1.docx");
        ArgumentCaptor<ApiInvocationRecordEntity> captor = ArgumentCaptor.forClass(ApiInvocationRecordEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getArtifactStorageKey()).isNull();
    }

    @Test
    void cleanExpiredDocumentArtifacts_skipsWhenNothingExpired() {
        when(repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(any(Instant.class)))
                .thenReturn(List.of());

        scheduler.cleanExpiredDocumentArtifacts();

        verify(objectStoragePort, never()).delete(anyString());
        verify(repository, never()).save(any());
    }

    private ApiInvocationRecordEntity sampleRecord(String externalId, String storageKey) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                UUID.randomUUID(),
                "TPL-001",
                UUID.randomUUID(),
                "svc-account",
                "req-1",
                "idem-1",
                "DEFAULT_ROUTE",
                null,
                "1.0.0",
                "DOCX",
                "SYNC_STREAM",
                "SUCCESS",
                null,
                "{}",
                "DOC-1",
                storageKey,
                true,
                now.plusSeconds(3600),
                now.plusSeconds(1800),
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                false,
                now,
                now
        );
    }

    private ApiInvocationRecordEntity expiredWithRecordExpiry(ApiInvocationRecordEntity record, Instant expiredAt) {
        return new ApiInvocationRecordEntity(
                record.getId(),
                record.getInvocationExternalId(),
                record.getInvocationKind(),
                record.getStatus(),
                record.getEnvironment(),
                record.getTemplateId(),
                record.getTemplateExternalId(),
                record.getCredentialId(),
                record.getAccessAccount(),
                record.getRequestId(),
                record.getIdempotencyKey(),
                record.getRouteType(),
                record.getRequestedReleaseVersion(),
                record.getResolvedReleaseVersion(),
                record.getOutputFormat(),
                record.getOutputMode(),
                record.getOutcome(),
                record.getDurationMs(),
                record.getParametersStorage(),
                record.getDocumentId(),
                record.getArtifactStorageKey(),
                record.isArtifactSaved(),
                expiredAt,
                record.getDocumentExpiresAt(),
                record.getBatchExternalId(),
                record.getParentInvocationExternalId(),
                record.getItemId(),
                record.getTaskExternalId(),
                record.getIdempotencyRecordId(),
                record.getAuditId(),
                record.isBatch(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private ApiInvocationRecordEntity artifactExpiredRecord(
            String externalId,
            String storageKey,
            Instant documentExpiresAt
    ) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                UUID.randomUUID(),
                "TPL-001",
                UUID.randomUUID(),
                "svc-account",
                "req-1",
                "idem-1",
                "DEFAULT_ROUTE",
                null,
                "1.0.0",
                "DOCX",
                "SYNC_STREAM",
                "SUCCESS",
                null,
                "{}",
                "DOC-1",
                storageKey,
                true,
                now.plusSeconds(86400),
                documentExpiresAt,
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                false,
                now,
                now
        );
    }
}
