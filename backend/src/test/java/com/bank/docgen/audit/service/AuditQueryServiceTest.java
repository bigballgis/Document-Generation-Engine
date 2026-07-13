package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuditQueryServiceTest extends AuditQueryServiceTestSupport {

    @Test
    void globalAdminQueriesManagementEventsWithoutGroupFilter() {
        allowAuditRead(globalAdmin);
        stubUnfilteredManagementPage(sampleManagementEvent());

        var result = queryManagement(globalAdmin, AuditReadActorRole.GLOBAL_ADMIN);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType()).isEqualTo("API_POLICY_UPDATED");
        assertThat(result.events().getFirst().templateDisplayName()).isNull();
        assertThat(result.events().getFirst().templateExternalId()).isNull();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(DEFAULT_SIZE);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void groupAdminRequiresTemplateAndGroupScope() {
        allowAuditRead(groupAdmin);

        assertThatThrownBy(() -> queryManagement(groupAdmin, AuditReadActorRole.GROUP_ADMIN))
                .isInstanceOf(AuditValidationException.class);
    }

    @Test
    void groupAdminQueriesManagementEventsWithinAuthorizedScope() {
        allowAuditRead(groupAdmin);
        stubRetailTemplateReadable(groupAdmin);
        stubGroupScopedManagementPage(null, "RETAIL", emptyPage());

        var result = queryManagement(
                groupAdmin, AuditReadActorRole.GROUP_ADMIN, TEMPLATE_ID, null, "RETAIL"
        );

        assertThat(result.events()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void rejectsInvalidTimeWindow() {
        Instant from = Instant.parse("2026-06-23T12:00:00Z");
        Instant to = Instant.parse("2026-06-23T10:00:00Z");

        assertThatThrownBy(() -> queryManagement(
                globalAdmin, AuditReadActorRole.GLOBAL_ADMIN, null, null, from, to, null, null
        )).isInstanceOf(AuditValidationException.class);
    }

    @Test
    void deniesAuditReadWhenCapabilityMissing() {
        denyAuditRead(globalAdmin);

        assertThatThrownBy(() -> queryManagement(globalAdmin, AuditReadActorRole.GLOBAL_ADMIN))
                .isInstanceOf(AuditAccessDeniedException.class);
    }

    @Test
    void auditAdminCanQuerySecurityLoginFailureEvents() {
        // BDD-LRP-D7-001 — SECURITY_* visible via existing management audit query scoping.
        allowAuditRead(auditAdmin);
        ManagementAuditEventEntity loginFailure = securityLoginFailureEvent();
        stubUnfilteredManagementPageByEventType(
                SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE, loginFailure
        );

        var result = queryManagement(
                auditAdmin,
                AuditReadActorRole.AUDIT_ADMIN,
                null,
                SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE,
                null
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType())
                .isEqualTo(SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE);
        assertThat(result.events().getFirst().actorSummary()).contains("Security audit");
        assertThat(result.events().getFirst().statusSummary()).containsIgnoringCase("failure");
    }

    @Test
    void templateAuthorCannotQuerySecurityEvents() {
        denyAuditRead(templateAuthor);

        assertThatThrownBy(() -> queryManagement(
                templateAuthor,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE,
                null
        )).isInstanceOf(AuditAccessDeniedException.class);
        verify(managementAuditEventRepository, never()).searchPaged(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        );
    }

    @Test
    void auditAdminCanQueryRetentionPurgeEvidence() {
        allowAuditRead(auditAdmin);
        ManagementAuditEventEntity purge = retentionPurgeEvent();
        stubUnfilteredManagementPageByEventType(
                AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE, purge
        );

        var result = queryManagement(
                auditAdmin,
                AuditReadActorRole.AUDIT_ADMIN,
                null,
                AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE,
                null
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType())
                .isEqualTo(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE);
        assertThat(result.events().getFirst().templateId()).isNull();
        assertThat(result.events().getFirst().statusSummary()).contains("deletedCount=");
    }

    @Test
    void groupAdminQueryScopesToGroup_soPlatformPurgeRowsAreExcluded() {
        allowAuditRead(groupAdmin);
        stubRetailTemplateReadable(groupAdmin);
        stubGroupScopedManagementPage(
                AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE, "RETAIL", emptyPage()
        );

        var result = queryManagement(
                groupAdmin,
                AuditReadActorRole.GROUP_ADMIN,
                TEMPLATE_ID,
                AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE,
                "RETAIL"
        );

        assertThat(result.events()).isEmpty();
        verify(managementAuditEventRepository).searchPaged(
                eq(TEMPLATE_ID),
                eq(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE),
                isNull(),
                isNull(),
                isNull(),
                eq("RETAIL"),
                eq(DEFAULT_PAGE),
                eq(DEFAULT_SIZE)
        );
    }

    @Test
    void exportManagementEventsUsesMaskedExportFormat() {
        allowAuditRead(globalAdmin);
        when(managementAuditEventRepository.search(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(sampleManagementEvent()));

        var result = exportManagement(globalAdmin, AuditReadActorRole.GLOBAL_ADMIN);

        assertThat(result.format()).isEqualTo(AuditQueryService.EXPORT_FORMAT);
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().actorSummaryMasked()).contains("****");
    }

    @Test
    void managementEventsEnrichTemplateDisplayFieldsInBatch() {
        allowAuditRead(globalAdmin);
        stubUnfilteredManagementPage(sampleManagementEvent());
        stubSampleTemplateDisplay();

        var result = queryManagement(globalAdmin, AuditReadActorRole.GLOBAL_ADMIN);

        assertThat(result.events().getFirst().templateId()).isEqualTo(TEMPLATE_ID.toString());
        assertThat(result.events().getFirst().templateDisplayName()).isEqualTo("Sample");
        assertThat(result.events().getFirst().templateExternalId()).isEqualTo("TPL-001");
        verify(templateService).lookupDisplayInfoByIds(Set.of(TEMPLATE_ID));
    }

    @Test
    void lifecycleEventsEnrichTemplateAndActorDisplayFieldsInBatch() {
        allowAuditRead(globalAdmin);
        TemplateLifecycleRecordEntity record = sampleLifecycleRecord();
        stubUnfilteredLifecyclePage(record);
        stubSampleTemplateDisplay();
        when(managementUserDisplayService.lookupDisplayNames(Set.of("10000003")))
                .thenReturn(Map.of("10000003", "Lifecycle Tester (10000003)"));

        var result = queryLifecycle(globalAdmin, AuditReadActorRole.GLOBAL_ADMIN);

        var event = result.events().getFirst();
        assertThat(event.templateDisplayName()).isEqualTo("Sample");
        assertThat(event.templateExternalId()).isEqualTo("TPL-001");
        assertThat(event.actorId()).isEqualTo("10000003");
        assertThat(event.actorDisplayName()).isEqualTo("Lifecycle Tester (10000003)");
        verify(templateService).lookupDisplayInfoByIds(Set.of(TEMPLATE_ID));
        verify(managementUserDisplayService).lookupDisplayNames(Set.of("10000003"));
    }

    @Test
    void globalAdminQueriesLifecycleEventsWithPagination() {
        allowAuditRead(globalAdmin);
        stubUnfilteredLifecyclePage(sampleLifecycleRecord());

        var result = queryLifecycle(globalAdmin, AuditReadActorRole.GLOBAL_ADMIN);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType()).isEqualTo("SUBMIT_FOR_TEST");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void groupAdminLifecycleExportRequiresScopedTemplate() {
        allowAuditRead(groupAdmin);
        stubRetailTemplateReadable(groupAdmin);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(TEMPLATE_ID)).thenReturn(List.of());

        var result = exportLifecycle(groupAdmin, AuditReadActorRole.GROUP_ADMIN, TEMPLATE_ID, "RETAIL");

        assertThat(result.format()).isEqualTo(AuditQueryService.LIFECYCLE_EXPORT_FORMAT);
        verify(templateService, times(2)).requireReadableTemplate(TEMPLATE_ID, groupAdmin);
    }

    @Test
    void managementEventsFilterByRequestIdQueriesRuntimeAuditStoreWithExactMatch() {
        allowAuditRead(globalAdmin);
        RuntimeGenerationAuditEventEntity entity = sampleRuntimeAuditEvent("req-audit-001");
        when(runtimeGenerationAuditEventRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("req-audit-001"),
                eq(DEFAULT_PAGE), eq(DEFAULT_SIZE)
        )).thenReturn(pageOf(entity));
        stubSampleTemplateDisplay();

        var result = queryManagement(
                globalAdmin, AuditReadActorRole.GLOBAL_ADMIN, null, null, null, null, null, "req-audit-001"
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().requestId()).isEqualTo("req-audit-001");
        assertThat(result.events().getFirst().eventType())
                .isEqualTo(RuntimeGenerationAuditRecorder.EVENT_SYNC_GENERATION);
        verify(runtimeGenerationAuditEventRepository).searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("req-audit-001"),
                eq(DEFAULT_PAGE), eq(DEFAULT_SIZE)
        );
        verify(managementAuditEventRepository, never()).searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(DEFAULT_PAGE), eq(DEFAULT_SIZE)
        );
    }

    @Test
    void lifecycleEventsReturnEmptyWhenRequestIdFilterProvided() {
        allowAuditRead(globalAdmin);

        var result = queryLifecycle(globalAdmin, AuditReadActorRole.GLOBAL_ADMIN, "req-audit-001");

        assertThat(result.events()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(lifecycleRecordRepository, never()).searchPaged(
                isNull(), isNull(), isNull(), isNull(), eq(DEFAULT_PAGE), eq(DEFAULT_SIZE)
        );
    }

    @Test
    void globalAdminQueriesGenerationEventsByExternalId() {
        allowAuditRead(globalAdmin);
        TemplateEntity template = templateEntity(
                "CORP", "CORP-FOL-OFFER", "Sample", TemplateLifecycleStatus.PUBLISHED
        );
        RuntimeGenerationAuditEventEntity runtimeEvent = corpGenerationAuditEvent();
        when(templateService.requireTemplateByExternalId("CORP-FOL-OFFER")).thenReturn(template);
        when(templateService.requireReadableTemplate(TEMPLATE_ID, globalAdmin)).thenReturn(template);
        when(runtimeGenerationAuditEventRepository.searchPaged(
                eq(TEMPLATE_ID),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(50)
        )).thenReturn(pageOf(runtimeEvent));

        var result = service.queryGenerationEventsByExternalId(globalAdmin, "CORP-FOL-OFFER", 0, 50);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().templateExternalId()).isEqualTo("CORP-FOL-OFFER");
        assertThat(result.content().getFirst().status()).isEqualTo("SUCCEEDED");
        assertThat(result.content().getFirst().outcome()).isEqualTo("SUCCESS");
        assertThat(result.content().getFirst().accessAccountSummary()).isEqualTo("e2****er");
    }
}
