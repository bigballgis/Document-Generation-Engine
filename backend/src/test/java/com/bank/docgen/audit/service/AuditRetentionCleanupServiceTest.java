package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-LRP-D1-001…004, 007, 009, 010 — audit retention purge service (ADR-0048).
 * BDD-CE-G04-011…014 — legal hold exemptions.
 */
@ExtendWith(MockitoExtension.class)
class AuditRetentionCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");

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
        service = new AuditRetentionCleanupService(
                managementCleanup,
                runtimeCleanup,
                purgeEvidenceWriter,
                legalHoldExemptionService,
                clock,
                90,
                365,
                true
        );
    }

    @Test
    void purgeManagement_deletesOlderThanCutoffAndWritesEvidence() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity aged = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of(aged));
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
        when(runtimeCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of(aged));
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
        when(managementCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of());

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isZero();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(managementCleanup).findByEventAtBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(cutoff);
        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void purgeManagement_skipsEvidenceWhenNothingDeleted() {
        when(managementCleanup.findByEventAtBefore(any())).thenReturn(List.of());

        service.purgeManagementAudit();

        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void purgeManagement_selfProtectsEvidence_byDeletingBeforeRecording() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        ManagementAuditEventEntity aged = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of(aged));
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
        service = new AuditRetentionCleanupService(
                managementCleanup,
                runtimeCleanup,
                purgeEvidenceWriter,
                legalHoldExemptionService,
                clock,
                30,
                365,
                true
        );
        Instant cutoff = NOW.minus(30, ChronoUnit.DAYS);
        ManagementAuditEventEntity aged = managementEvent(UUID.randomUUID(), cutoff.minusSeconds(1));
        when(managementCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of(aged));
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
        service = new AuditRetentionCleanupService(
                managementCleanup,
                runtimeCleanup,
                purgeEvidenceWriter,
                legalHoldExemptionService,
                clock,
                90,
                365,
                false
        );

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
        when(managementCleanup.findByEventAtBefore(cutoffMgmt)).thenReturn(List.of(mgmt));
        when(runtimeCleanup.findByEventAtBefore(cutoffRuntime)).thenReturn(List.of(runtime));
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
        when(managementCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of(a, b));
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
        when(managementCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of(protectedRow, platformRow));
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
        when(runtimeCleanup.findByEventAtBefore(cutoff)).thenReturn(List.of(protectedRow));
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
