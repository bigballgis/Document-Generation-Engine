package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.persistence.ManagementAuditEventCleanupRepository;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventCleanupRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-LRP-D1-001…004, 007, 009, 010 — audit retention purge service (ADR-0048).
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
                clock,
                90,
                365,
                true
        );
    }

    @Test
    void purgeManagement_deletesOlderThanCutoffAndWritesEvidence() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        when(managementCleanup.deleteOlderThan(cutoff)).thenReturn(3);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isEqualTo(3);
        verify(managementCleanup).deleteOlderThan(cutoff);
        verify(purgeEvidenceWriter).write(
                eq(AuditRetentionCleanupService.MANAGEMENT_TABLE),
                eq(90),
                eq(cutoff),
                eq(3)
        );
    }

    @Test
    void purgeRuntime_deletesOlderThanCutoffAndWritesEvidence() {
        Instant cutoff = NOW.minus(365, ChronoUnit.DAYS);
        when(runtimeCleanup.deleteOlderThan(cutoff)).thenReturn(2);

        int deleted = service.purgeRuntimeAudit();

        assertThat(deleted).isEqualTo(2);
        verify(runtimeCleanup).deleteOlderThan(cutoff);
        verify(purgeEvidenceWriter).write(
                eq(AuditRetentionCleanupService.RUNTIME_TABLE),
                eq(365),
                eq(cutoff),
                eq(2)
        );
    }

    @Test
    void purgeManagement_retainsBoundaryAndInWindow_whenDeletePredicateIsStrictLessThan() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        when(managementCleanup.deleteOlderThan(cutoff)).thenReturn(0);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isZero();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(managementCleanup).deleteOlderThan(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(cutoff);
        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void purgeManagement_skipsEvidenceWhenNothingDeleted() {
        when(managementCleanup.deleteOlderThan(any())).thenReturn(0);

        service.purgeManagementAudit();

        verify(purgeEvidenceWriter, never()).write(any(), any(Integer.class), any(), any(Integer.class));
    }

    @Test
    void purgeManagement_selfProtectsEvidence_byDeletingBeforeRecording() {
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        when(managementCleanup.deleteOlderThan(cutoff)).thenReturn(1);

        service.purgeManagementAudit();

        var inOrder = org.mockito.Mockito.inOrder(managementCleanup, purgeEvidenceWriter);
        inOrder.verify(managementCleanup).deleteOlderThan(cutoff);
        inOrder.verify(purgeEvidenceWriter).write(
                AuditRetentionCleanupService.MANAGEMENT_TABLE,
                90,
                cutoff,
                1
        );
        verify(managementCleanup).deleteOlderThan(any());
    }

    @Test
    void purgeManagement_usesConfiguredRetentionDays() {
        service = new AuditRetentionCleanupService(
                managementCleanup,
                runtimeCleanup,
                purgeEvidenceWriter,
                clock,
                30,
                365,
                true
        );
        Instant cutoff = NOW.minus(30, ChronoUnit.DAYS);
        when(managementCleanup.deleteOlderThan(cutoff)).thenReturn(1);

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
        when(managementCleanup.deleteOlderThan(any())).thenReturn(1);
        when(runtimeCleanup.deleteOlderThan(any())).thenReturn(1);

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
        // BDD-LRP-D7-009: SECURITY_* rows live on management_audit_event and are purged by event_at
        // cutoff with no event_type filter (ADR-0048 / LR-D1).
        Instant cutoff = NOW.minus(90, ChronoUnit.DAYS);
        when(managementCleanup.deleteOlderThan(cutoff)).thenReturn(2);

        int deleted = service.purgeManagementAudit();

        assertThat(deleted).isEqualTo(2);
        verify(managementCleanup).deleteOlderThan(cutoff);
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
}
