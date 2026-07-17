package com.bank.docgen.runtime.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.legalhold.service.LegalHoldExemptionService;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * ADR-0040 / CE-G04 invocation retention cleanup.
 * BDD-PRR-A01-004 / 005 — bounded Pageable batch delete + legal-hold.
 */
@ExtendWith(MockitoExtension.class)
class InvocationRetentionCleanupSchedulerTest {

    private static final int BATCH_SIZE = 500;

    @Mock
    private ApiInvocationRecordRepository repository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private LegalHoldExemptionService legalHoldExemptionService;

    private InvocationRetentionCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new InvocationRetentionCleanupScheduler(
                repository,
                objectStoragePort,
                legalHoldExemptionService,
                BATCH_SIZE
        );
    }

    @Test
    void cleanExpiredRecords_deletesPastRecordExpiryRows() {
        ApiInvocationRecordEntity expired = sampleRecord("INV-EXPIRED1", "storage/doc.docx");
        expired = expiredWithRecordExpiry(expired, Instant.now().minusSeconds(60));
        when(repository.findByRecordExpiresAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isInvocationExempt(any(), any(), any())).thenReturn(false);

        scheduler.cleanExpiredRecords();

        verify(repository).deleteAll(List.of(expired));
        verify(objectStoragePort, never()).delete(anyString());
    }

    @Test
    void cleanExpiredRecords_destroysParametersStorageWithInvocationRow_adr0057() {
        ApiInvocationRecordEntity expired = sampleRecordWithParameters(
                "INV-PARAMS01",
                "{\"variables\":{\"name\":\"Alice\"},\"encryption\":{\"enabled\":false}}"
        );
        expired = expiredWithRecordExpiry(expired, Instant.now().minusSeconds(30));
        when(repository.findByRecordExpiresAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isInvocationExempt(any(), any(), any())).thenReturn(false);

        scheduler.cleanExpiredRecords();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ApiInvocationRecordEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).deleteAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getParametersStorage()).contains("Alice");
        assertThat(captor.getValue().get(0).getInvocationExternalId()).isEqualTo("INV-PARAMS01");
    }

    @Test
    void cleanExpiredRecords_skipsLegalHoldProtectedInvocation() {
        ApiInvocationRecordEntity protectedRow = expiredWithRecordExpiry(
                sampleRecord("INV-HOLD1", "storage/doc.docx"),
                Instant.now().minusSeconds(60)
        );
        ApiInvocationRecordEntity unprotected = expiredWithRecordExpiry(
                sampleRecord("INV-FREE1", "storage/doc2.docx"),
                Instant.now().minusSeconds(60)
        );
        when(repository.findByRecordExpiresAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(protectedRow, unprotected))
                .thenReturn(List.of(protectedRow))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isInvocationExempt(
                protectedRow.getTemplateId(),
                "INV-HOLD1",
                protectedRow.getCreatedAt()
        )).thenReturn(true);
        when(legalHoldExemptionService.isInvocationExempt(
                unprotected.getTemplateId(),
                "INV-FREE1",
                unprotected.getCreatedAt()
        )).thenReturn(false);

        scheduler.cleanExpiredRecords();

        verify(repository).deleteAll(List.of(unprotected));
    }

    @Test
    void cleanExpiredDocumentArtifacts_deletesStorageAndClearsKey() {
        ApiInvocationRecordEntity artifactExpired = artifactExpiredRecord(
                "INV-DOCEXP1",
                "generated/doc-1.docx",
                Instant.now().minusSeconds(60)
        );
        when(repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(
                any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(artifactExpired))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isInvocationExempt(any(), any(), any())).thenReturn(false);
        when(objectStoragePort.exists("generated/doc-1.docx")).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.cleanExpiredDocumentArtifacts();

        verify(objectStoragePort).delete("generated/doc-1.docx");
        ArgumentCaptor<ApiInvocationRecordEntity> captor = ArgumentCaptor.forClass(ApiInvocationRecordEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getArtifactStorageKey()).isNull();
    }

    @Test
    void cleanExpiredDocumentArtifacts_skipsLegalHoldProtectedArtifact() {
        ApiInvocationRecordEntity artifactExpired = artifactExpiredRecord(
                "INV-HOLD-ART",
                "generated/doc-hold.docx",
                Instant.now().minusSeconds(60)
        );
        when(repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(
                any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(artifactExpired))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isInvocationExempt(any(), any(), any())).thenReturn(true);

        scheduler.cleanExpiredDocumentArtifacts();

        verify(objectStoragePort, never()).delete(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void cleanExpiredDocumentArtifacts_skipsWhenNothingExpired() {
        when(repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(
                any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        scheduler.cleanExpiredDocumentArtifacts();

        verify(objectStoragePort, never()).delete(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void cleanExpiredRecords_largeCandidateSet_usesBoundedBatches() {
        // BDD-PRR-A01-004
        List<ApiInvocationRecordEntity> all = IntStream.range(0, BATCH_SIZE * 3)
                .mapToObj(i -> expiredWithRecordExpiry(
                        sampleRecord("INV-LARGE-" + i, "storage/doc-" + i + ".docx"),
                        Instant.now().minusSeconds(60)
                ))
                .toList();
        when(repository.findByRecordExpiresAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(all.subList(0, BATCH_SIZE))
                .thenReturn(all.subList(BATCH_SIZE, BATCH_SIZE * 2))
                .thenReturn(all.subList(BATCH_SIZE * 2, BATCH_SIZE * 3))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isInvocationExempt(any(), any(), any())).thenReturn(false);

        scheduler.cleanExpiredRecords();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(4)).findByRecordExpiresAtBefore(any(Instant.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allMatch(pageable -> pageable.getPageSize() == BATCH_SIZE);
        verify(repository, times(3)).deleteAll(any());
    }

    @Test
    void cleanExpiredDocumentArtifacts_largeCandidateSet_usesBoundedBatches() {
        // BDD-PRR-A01-005
        List<ApiInvocationRecordEntity> all = IntStream.range(0, BATCH_SIZE * 3)
                .mapToObj(i -> artifactExpiredRecord(
                        "INV-ART-" + i,
                        "generated/doc-" + i + ".docx",
                        Instant.now().minusSeconds(60)
                ))
                .toList();
        when(repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(
                any(Instant.class), any(Pageable.class)))
                .thenReturn(all.subList(0, BATCH_SIZE))
                .thenReturn(all.subList(BATCH_SIZE, BATCH_SIZE * 2))
                .thenReturn(all.subList(BATCH_SIZE * 2, BATCH_SIZE * 3))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isInvocationExempt(any(), any(), any())).thenReturn(false);
        when(objectStoragePort.exists(anyString())).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.cleanExpiredDocumentArtifacts();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(4)).findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(
                any(Instant.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allMatch(pageable -> pageable.getPageSize() == BATCH_SIZE);
        verify(repository, times(BATCH_SIZE * 3)).save(any());
    }

    private ApiInvocationRecordEntity sampleRecord(String externalId, String storageKey) {
        return sampleRecordWithParameters(externalId, "{}");
    }

    private ApiInvocationRecordEntity sampleRecordWithParameters(String externalId, String parametersStorage) {
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
                parametersStorage,
                "DOC-1",
                "storage/doc.docx",
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
