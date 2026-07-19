package com.bank.docgen.legalhold.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.persistence.ManagementAuditEventCleanupRepository;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.service.AuditRetentionCleanupService;
import com.bank.docgen.audit.service.AuditRetentionPurgeEvidenceWriter;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import com.bank.docgen.legalhold.persistence.LegalHoldRepository;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventCleanupRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.scheduler.InvocationRetentionCleanupScheduler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * IBL-D5 / F23 — real {@link LegalHoldExemptionService} bridged into retention cleaners
 * (enforce hit + block delete paths), not a mocked exemption boolean.
 */
@ExtendWith(MockitoExtension.class)
class LegalHoldRetentionEnforceBridgeTest {

    private static final UUID TEMPLATE_T = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
    private static final UUID TEMPLATE_OTHER = UUID.fromString("ffffffff-1111-4222-8333-444444444444");
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-30T23:59:59Z");
    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");
    private static final Instant IN_WINDOW = Instant.parse("2026-03-15T12:00:00Z");

    @Mock
    private LegalHoldRepository legalHoldRepository;
    @Mock
    private ApiInvocationRecordRepository invocationRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private ManagementAuditEventCleanupRepository managementCleanup;
    @Mock
    private RuntimeGenerationAuditEventCleanupRepository runtimeCleanup;
    @Mock
    private AuditRetentionPurgeEvidenceWriter purgeEvidenceWriter;

    private LegalHoldExemptionService exemptionService;
    private InvocationRetentionCleanupScheduler invocationScheduler;
    private AuditRetentionCleanupService auditCleanup;

    @BeforeEach
    void setUp() {
        exemptionService = new LegalHoldExemptionService(legalHoldRepository);
        invocationScheduler = new InvocationRetentionCleanupScheduler(
                invocationRepository,
                objectStoragePort,
                exemptionService,
                500
        );
        auditCleanup = new AuditRetentionCleanupService(
                managementCleanup,
                runtimeCleanup,
                purgeEvidenceWriter,
                exemptionService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                90,
                365,
                true,
                1000
        );
    }

    @Test
    void enforce_activeTemplateWindow_blocksInvocationRecordDelete() {
        when(legalHoldRepository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(templateWindowHold(TO)));
        ApiInvocationRecordEntity protectedRow = invocation(
                TEMPLATE_T, "INV-HELD", IN_WINDOW, NOW.minusSeconds(60));
        ApiInvocationRecordEntity freeRow = invocation(
                TEMPLATE_OTHER, "INV-FREE", IN_WINDOW, NOW.minusSeconds(60));
        when(invocationRepository.findByRecordExpiresAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(protectedRow, freeRow))
                .thenReturn(List.of(protectedRow))
                .thenReturn(List.of());

        invocationScheduler.cleanExpiredRecords();

        verify(invocationRepository).deleteAll(List.of(freeRow));
    }

    @Test
    void block_releasedHold_allowsInvocationRecordDelete() {
        when(legalHoldRepository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of());
        ApiInvocationRecordEntity expired = invocation(
                TEMPLATE_T, "INV-WAS-HELD", IN_WINDOW, NOW.minusSeconds(60));
        when(invocationRepository.findByRecordExpiresAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());

        invocationScheduler.cleanExpiredRecords();

        verify(invocationRepository).deleteAll(List.of(expired));
    }

    @Test
    void block_outOfWindow_allowsInvocationRecordDelete() {
        when(legalHoldRepository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(templateWindowHold(TO)));
        Instant outOfWindow = Instant.parse("2026-08-01T00:00:00Z");
        ApiInvocationRecordEntity expired = invocation(
                TEMPLATE_T, "INV-LATE", outOfWindow, NOW.minusSeconds(60));
        when(invocationRepository.findByRecordExpiresAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());

        invocationScheduler.cleanExpiredRecords();

        verify(invocationRepository).deleteAll(List.of(expired));
    }

    @Test
    void enforce_activeInvocationSet_blocksArtifactCleanup() {
        when(legalHoldRepository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("INV-ART-HOLD"))));
        ApiInvocationRecordEntity heldArtifact = artifactExpired(
                TEMPLATE_T, "INV-ART-HOLD", "generated/held.docx", NOW.minusSeconds(30));
        when(invocationRepository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(
                any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(heldArtifact))
                .thenReturn(List.of());

        invocationScheduler.cleanExpiredDocumentArtifacts();

        verify(objectStoragePort, never()).delete(any());
        verify(invocationRepository, never()).save(any());
    }

    @Test
    void enforce_activeTemplateWindow_blocksManagementAuditPurge() {
        when(legalHoldRepository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(templateWindowHold(TO)));
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity held = managementEvent(TEMPLATE_T, IN_WINDOW);
        ManagementAuditEventEntity platform = managementEvent(null, cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(held, platform))
                .thenReturn(List.of(held))
                .thenReturn(List.of());

        int deleted = auditCleanup.purgeManagementAudit();

        assertThat(deleted).isEqualTo(1);
        verify(managementCleanup).deleteAll(List.of(platform));
    }

    @Test
    void block_invocationSet_doesNotBlockManagementAuditPurge() {
        when(legalHoldRepository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("INV-1"))));
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity aged = managementEvent(TEMPLATE_T, cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(aged))
                .thenReturn(List.of());

        int deleted = auditCleanup.purgeManagementAudit();

        assertThat(deleted).isEqualTo(1);
        verify(managementCleanup).deleteAll(List.of(aged));
    }

    @Test
    void enforce_activeInvocationSet_blocksRuntimeAuditPurgeByTaskId() {
        when(legalHoldRepository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("inv-runtime-1"))));
        Instant cutoff = NOW.minus(365, ChronoUnit.DAYS);
        RuntimeGenerationAuditEventEntity held = runtimeEvent(
                cutoff.minusSeconds(1), TEMPLATE_OTHER, "inv-runtime-1", null);
        RuntimeGenerationAuditEventEntity free = runtimeEvent(
                cutoff.minusSeconds(2), TEMPLATE_OTHER, "other", null);
        when(runtimeCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(held, free))
                .thenReturn(List.of(held))
                .thenReturn(List.of());

        int deleted = auditCleanup.purgeRuntimeAudit();

        assertThat(deleted).isEqualTo(1);
        verify(runtimeCleanup).deleteAll(List.of(free));
    }

    private LegalHoldEntity templateWindowHold(Instant effectiveTo) {
        return new LegalHoldEntity(
                UUID.randomUUID(),
                "HOLD-BRIDGE-TW",
                LegalHoldScopeType.TEMPLATE_WINDOW,
                LegalHoldStatus.ACTIVE,
                "litigation",
                TEMPLATE_T,
                "TPL-001",
                FROM,
                effectiveTo,
                Instant.parse("2026-01-02T00:00:00Z"),
                "10000001",
                Set.of()
        );
    }

    private LegalHoldEntity invocationSetHold(Set<String> ids) {
        return new LegalHoldEntity(
                UUID.randomUUID(),
                "HOLD-BRIDGE-IS",
                LegalHoldScopeType.INVOCATION_SET,
                LegalHoldStatus.ACTIVE,
                "preserve",
                null,
                null,
                null,
                null,
                Instant.parse("2026-01-02T00:00:00Z"),
                "10000001",
                ids
        );
    }

    private ApiInvocationRecordEntity invocation(
            UUID templateId,
            String externalId,
            Instant createdAt,
            Instant recordExpiresAt
    ) {
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                templateId,
                "TPL-X",
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
                "storage/doc.docx",
                true,
                recordExpiresAt,
                createdAt.plusSeconds(1800),
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                false,
                createdAt,
                createdAt
        );
    }

    private ApiInvocationRecordEntity artifactExpired(
            UUID templateId,
            String externalId,
            String storageKey,
            Instant documentExpiresAt
    ) {
        Instant createdAt = IN_WINDOW;
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                templateId,
                "TPL-X",
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
                createdAt.plusSeconds(86400),
                documentExpiresAt,
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                false,
                createdAt,
                createdAt
        );
    }

    private ManagementAuditEventEntity managementEvent(UUID templateId, Instant eventAt) {
        return new ManagementAuditEventEntity(
                UUID.randomUUID(),
                eventAt,
                "SOME_EVENT",
                templateId,
                null,
                null,
                null,
                null,
                "[]",
                false,
                null,
                "10000001",
                "Admin",
                null,
                "summary",
                "[]"
        );
    }

    private RuntimeGenerationAuditEventEntity runtimeEvent(
            Instant eventAt,
            UUID templateId,
            String taskExternalId,
            String documentId
    ) {
        return new RuntimeGenerationAuditEventEntity(
                UUID.randomUUID(),
                eventAt,
                "GENERATION",
                "dev",
                templateId,
                "RETAIL",
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                taskExternalId,
                null,
                documentId,
                "SUCCESS",
                null,
                null,
                null,
                "audit-1",
                "trace-1"
        );
    }
}
