package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.persistence.ManagementAuditEventCleanupRepository;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.legalhold.service.LegalHoldExemptionService;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventCleanupRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
 * BDD-LRP-D1-001…004, 007, 009, 010 — audit retention purge service (ADR-0048).
 * BDD-CE-G04-011…014 — legal hold exemptions.
 * BDD-PRR-A01-001…003, 006, 007 — bounded Pageable batch delete.
 */
@ExtendWith(MockitoExtension.class)
class AuditRetentionCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");
    private static final int BATCH_SIZE = 1000;

    @Mock
    private ManagementAuditEventCleanupRepository managementCleanup;
    @Mock
    private RuntimeGenerationAuditEventCleanupRepository runtimeCleanup;
    @Mock
    private AuditRetentionPurgeEvidenceWriter purgeEvidenceWriter;
    @Mock
    private LegalHoldExemptionService legalHoldExemptionService;
    @Mock
    private ApiInvocationRecordRepository invocationRecordRepository;

    private Clock clock;
    private AuditRetentionCleanupService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = newService(90, 365, true, BATCH_SIZE);
    }

    @Test
    void purgeManagement_deletesOlderThanCutoffAndWritesEvidence() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity aged = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(aged))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(any(), any())).thenReturn(false);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isEqualTo(1);
        verify(managementCleanup).deleteAll(List.of(aged));
        verify(purgeEvidenceWriter).write(
                eq(AuditRetentionCleanupService.MANAGEMENT_TABLE),
                eq(90),
                eq(cutoff),
                eq(1)
        );
    }

    @Test
    void purgeRuntime_deletesOlderThanCutoffAndWritesEvidence() {
        Instant cutoff = NOW.minus(365, ChronoUnit.DAYS);
        RuntimeGenerationAuditEventEntity aged = runtimeEvent(cutoff.minusSeconds(1), null, null);
        when(runtimeCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(aged))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isRuntimeAuditExempt(any(), any(), any(), any())).thenReturn(false);

        int deleted = service.purgeRuntimeAudit();

        assertThat(deleted).isEqualTo(1);
        verify(runtimeCleanup).deleteAll(List.of(aged));
        verify(purgeEvidenceWriter).write(
                eq(AuditRetentionCleanupService.RUNTIME_TABLE),
                eq(365),
                eq(cutoff),
                eq(1)
        );
    }

    @Test
    void purgeManagement_retainsBoundaryAndInWindow_whenDeletePredicateIsStrictLessThan() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class))).thenReturn(List.of());

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isZero();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(managementCleanup).findByEventAtBefore(cutoffCaptor.capture(), any(Pageable.class));
        assertThat(cutoffCaptor.getValue()).isEqualTo(cutoff);
        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void purgeManagement_skipsEvidenceWhenNothingDeleted() {
        when(managementCleanup.findByEventAtBefore(any(), any(Pageable.class))).thenReturn(List.of());

        service.purgeManagementAudit();

        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void purgeManagement_selfProtectsEvidence_byDeletingBeforeRecording() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity aged = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(aged))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(any(), any())).thenReturn(false);

        service.purgeManagementAudit();

        var inOrder = org.mockito.Mockito.inOrder(managementCleanup, purgeEvidenceWriter);
        inOrder.verify(managementCleanup).deleteAll(List.of(aged));
        inOrder.verify(purgeEvidenceWriter).write(
                AuditRetentionCleanupService.MANAGEMENT_TABLE,
                90,
                cutoff,
                1
        );
    }

    @Test
    void purgeManagement_usesConfiguredRetentionDays() {
        service = newService(30, 365, true, BATCH_SIZE);
        Instant cutoff = NOW.minus(30, ChronoUnit.DAYS);
        ManagementAuditEventEntity aged = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(aged))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(any(), any())).thenReturn(false);

        service.purgeManagementAudit();

        verify(purgeEvidenceWriter).write(
                AuditRetentionCleanupService.MANAGEMENT_TABLE,
                30,
                cutoff,
                1
        );
    }

    @Test
    void purgeWhenDisabled_deletesNothingAndWritesNoEvidence() {
        service = newService(90, 365, false, BATCH_SIZE);

        assertThat(service.purgeManagementAudit()).isZero();
        assertThat(service.purgeRuntimeAudit()).isZero();
        verifyNoInteractions(managementCleanup, runtimeCleanup, purgeEvidenceWriter);
    }

    @Test
    void purgeDoesNotTouchInvocationRecords() {
        Instant cutoffMgmt = NOW.minus(90, ChronoUnit.DAYS);
        Instant cutoffRuntime = NOW.minus(365, ChronoUnit.DAYS);
        ManagementAuditEventEntity mgmt = managementEvent(UUID.randomUUID(), cutoffMgmt.minusSeconds(1));
        RuntimeGenerationAuditEventEntity runtime = runtimeEvent(cutoffRuntime.minusSeconds(1), null, null);
        when(managementCleanup.findByEventAtBefore(eq(cutoffMgmt), any(Pageable.class)))
                .thenReturn(List.of(mgmt))
                .thenReturn(List.of());
        when(runtimeCleanup.findByEventAtBefore(eq(cutoffRuntime), any(Pageable.class)))
                .thenReturn(List.of(runtime))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(any(), any())).thenReturn(false);
        when(legalHoldExemptionService.isRuntimeAuditExempt(any(), any(), any(), any())).thenReturn(false);

        service.purgeManagementAudit();
        service.purgeRuntimeAudit();

        verifyNoInteractions(invocationRecordRepository);
    }

    @Test
    void recordRetentionPurge_contractConstantsMatchSpec() {
        assertThat(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE).isEqualTo("AUDIT_RETENTION_PURGE");
        assertThat(AuditRetentionPurgeEvidenceWriter.ACTOR_USERNAME).isEqualTo("SYSTEM");
        assertThat(AuditRetentionCleanupService.MANAGEMENT_TABLE).isEqualTo("management_audit_event");
        assertThat(AuditRetentionCleanupService.RUNTIME_TABLE).isEqualTo("runtime_generation_audit_event");
    }

    @Test
    void purgeManagement_coversSecurityEventTypesOnSameTableCutoff() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity a = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(2));
        ManagementAuditEventEntity b = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(a, b))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(any(), any())).thenReturn(false);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isEqualTo(2);
        verify(managementCleanup).deleteAll(List.of(a, b));
        assertThat(SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE)
                .isEqualTo("SECURITY_LOGIN_FAILURE");
        assertThat(SecurityManagementAuditRecorder.SECURITY_ROUTE_ACCESS_DENIED)
                .isEqualTo("SECURITY_ROUTE_ACCESS_DENIED");
        assertThat(SecurityManagementAuditRecorder.SECURITY_DOCUMENT_DOWNLOAD)
                .isEqualTo("SECURITY_DOCUMENT_DOWNLOAD");
        assertThat(SecurityManagementAuditRecorder.SECURITY_DOCUMENT_DOWNLOAD_DENIED)
                .isEqualTo("SECURITY_DOCUMENT_DOWNLOAD_DENIED");
        assertThat(AuditRetentionCleanupService.MANAGEMENT_TABLE).isEqualTo("management_audit_event");
    }

    @Test
    void purgeManagement_skipsLegalHoldExemptRows() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        UUID templateId = UUID.randomUUID();
        ManagementAuditEventEntity protectedRow = managementEvent(templateId, cutoff.minusSeconds(1));
        ManagementAuditEventEntity platformRow = managementEvent(null, cutoff.minusSeconds(2));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(protectedRow, platformRow))
                .thenReturn(List.of(protectedRow))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(templateId, protectedRow.getEventAt()))
                .thenReturn(true);
        when(legalHoldExemptionService.isManagementAuditExempt(null, platformRow.getEventAt()))
                .thenReturn(false);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isEqualTo(1);
        verify(managementCleanup).deleteAll(List.of(platformRow));
    }

    @Test
    void purgeRuntime_skipsLegalHoldExemptRows() {
        Instant cutoff = NOW.minus(365, ChronoUnit.DAYS);
        RuntimeGenerationAuditEventEntity protectedRow = runtimeEvent(cutoff.minusSeconds(1), "inv-1", null);
        when(runtimeCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(protectedRow))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isRuntimeAuditExempt(
                protectedRow.getTemplateId(),
                protectedRow.getEventAt(),
                "inv-1",
                null
        )).thenReturn(true);

        int deleted = service.purgeRuntimeAudit();

        assertThat(deleted).isZero();
        verify(runtimeCleanup, never()).deleteAll(any());
        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void purgeManagement_largeCandidateSet_usesBoundedBatchesAndDeletesAll() {
        // BDD-PRR-A01-001
        int batchSize = 500;
        service = newService(90, 365, true, batchSize);
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        List<ManagementAuditEventEntity> all = IntStream.range(0, batchSize * 3)
                .mapToObj(i -> managementEvent(UUID.randomUUID(), cutoff.minusSeconds(i + 1)))
                .toList();
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(all.subList(0, batchSize))
                .thenReturn(all.subList(batchSize, batchSize * 2))
                .thenReturn(all.subList(batchSize * 2, batchSize * 3))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(any(), any())).thenReturn(false);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isEqualTo(batchSize * 3);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(managementCleanup, times(4)).findByEventAtBefore(eq(cutoff), pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allMatch(pageable -> pageable.getPageSize() == batchSize);
        verify(managementCleanup, times(3)).deleteAll(any());
        verify(purgeEvidenceWriter).write(
                AuditRetentionCleanupService.MANAGEMENT_TABLE,
                90,
                cutoff,
                batchSize * 3
        );
    }

    @Test
    void purgeRuntime_largeCandidateSet_usesBoundedBatches() {
        // BDD-PRR-A01-002
        int batchSize = 500;
        service = newService(90, 365, true, batchSize);
        Instant cutoff = NOW.minus(365, ChronoUnit.DAYS);
        List<RuntimeGenerationAuditEventEntity> all = IntStream.range(0, batchSize * 3)
                .mapToObj(i -> runtimeEvent(cutoff.minusSeconds(i + 1), null, null))
                .toList();
        when(runtimeCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(all.subList(0, batchSize))
                .thenReturn(all.subList(batchSize, batchSize * 2))
                .thenReturn(all.subList(batchSize * 2, batchSize * 3))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isRuntimeAuditExempt(any(), any(), any(), any())).thenReturn(false);

        int deleted = service.purgeRuntimeAudit();

        assertThat(deleted).isEqualTo(batchSize * 3);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(runtimeCleanup, atLeastOnce()).findByEventAtBefore(eq(cutoff), pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allMatch(pageable -> pageable.getPageSize() == batchSize);
        verify(purgeEvidenceWriter).write(
                AuditRetentionCleanupService.RUNTIME_TABLE,
                365,
                cutoff,
                batchSize * 3
        );
    }

    @Test
    void purgeManagement_allExempt_writesNoPurgeEvidence() {
        // BDD-PRR-A01-006
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity held = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(held))
                .thenReturn(List.of());
        when(legalHoldExemptionService.isManagementAuditExempt(any(), any())).thenReturn(true);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isZero();
        verify(managementCleanup, never()).deleteAll(any());
        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void cleanupBatchSize_windowEndpointsAccepted() {
        // BDD-PRR-A01-007
        assertThat(newService(90, 365, true, 500).cleanupBatchSize()).isEqualTo(500);
        assertThat(newService(90, 365, true, 2000).cleanupBatchSize()).isEqualTo(2000);
    }

    @Test
    void cleanupBatchSize_outOfWindow_failsStartup() {
        assertThatThrownBy(() -> newService(90, 365, true, 499))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cleanup-batch-size");
        assertThatThrownBy(() -> newService(90, 365, true, 2001))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cleanup-batch-size");
    }

    @Test
    void defaultCleanupBatchSize_is1000() {
        assertThat(AuditRetentionCleanupService.DEFAULT_CLEANUP_BATCH_SIZE).isEqualTo(1000);
        assertThat(service.cleanupBatchSize()).isEqualTo(1000);
    }

    private AuditRetentionCleanupService newService(
            int managementDays,
            int runtimeDays,
            boolean enabled,
            int batchSize
    ) {
        return new AuditRetentionCleanupService(
                managementCleanup,
                runtimeCleanup,
                purgeEvidenceWriter,
                legalHoldExemptionService,
                clock,
                managementDays,
                runtimeDays,
                enabled,
                batchSize
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
            String taskExternalId,
            String documentId
    ) {
        return new RuntimeGenerationAuditEventEntity(
                UUID.randomUUID(),
                eventAt,
                "GENERATION",
                "dev",
                UUID.randomUUID(),
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
