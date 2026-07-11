package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateService.TemplateDisplayInfo;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private ManagementAuditEventRepository managementAuditEventRepository;
    @Mock
    private RuntimeGenerationAuditEventRepository runtimeGenerationAuditEventRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private TemplateService templateService;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    private AuditQueryService service;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        service = new AuditQueryService(
                managementAuditEventRepository,
                runtimeGenerationAuditEventRepository,
                lifecycleRecordRepository,
                templateService,
                managementUserDisplayService,
                groupAccessService,
                new AuditMaskingService(),
                new ObjectMapper()
        );
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
    }

    @Test
    void globalAdminQueriesManagementEventsWithoutGroupFilter() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);
        ManagementAuditEventEntity entity = sampleManagementEvent();
        when(managementAuditEventRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 1, 1));

        var result = service.queryManagementEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType()).isEqualTo("API_POLICY_UPDATED");
        assertThat(result.events().getFirst().templateDisplayName()).isNull();
        assertThat(result.events().getFirst().templateExternalId()).isNull();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void groupAdminRequiresTemplateAndGroupScope() {
        when(groupAccessService.canReadAudit(groupAdmin)).thenReturn(true);

        assertThatThrownBy(() -> service.queryManagementEvents(
                groupAdmin,
                AuditReadActorRole.GROUP_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        )).isInstanceOf(AuditValidationException.class);
    }

    @Test
    void groupAdminQueriesManagementEventsWithinAuthorizedScope() {
        when(groupAccessService.canReadAudit(groupAdmin)).thenReturn(true);
        TemplateEntity template = templateEntity("RETAIL");
        when(templateService.requireReadableTemplate(TEMPLATE_ID, groupAdmin)).thenReturn(template);
        when(managementAuditEventRepository.searchPaged(
                eq(TEMPLATE_ID), isNull(), isNull(), isNull(), isNull(), eq("RETAIL"), eq(0), eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(), 0, 0));

        var result = service.queryManagementEvents(
                groupAdmin,
                AuditReadActorRole.GROUP_ADMIN,
                TEMPLATE_ID,
                null,
                null,
                null,
                null,
                "RETAIL",
                null,
                0,
                20
        );

        assertThat(result.events()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void rejectsInvalidTimeWindow() {
        Instant from = Instant.parse("2026-06-23T12:00:00Z");
        Instant to = Instant.parse("2026-06-23T10:00:00Z");

        assertThatThrownBy(() -> service.queryManagementEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                from,
                to,
                null,
                null,
                0,
                20
        )).isInstanceOf(AuditValidationException.class);
    }

    @Test
    void deniesAuditReadWhenCapabilityMissing() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(false);

        assertThatThrownBy(() -> service.queryManagementEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        )).isInstanceOf(AuditAccessDeniedException.class);
    }

    @Test
    void auditAdminCanQuerySecurityLoginFailureEvents() {
        // BDD-LRP-D7-001 — SECURITY_* visible via existing management audit query scoping.
        ManagementSessionClaims auditAdmin = session("10000004", List.of("AUDIT_ADMIN"), List.of("*"));
        when(groupAccessService.canReadAudit(auditAdmin)).thenReturn(true);
        ManagementAuditEventEntity loginFailure = securityLoginFailureEvent();
        when(managementAuditEventRepository.searchPaged(
                isNull(),
                eq(SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(loginFailure), 1, 1));

        var result = service.queryManagementEvents(
                auditAdmin,
                AuditReadActorRole.AUDIT_ADMIN,
                null,
                SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType())
                .isEqualTo(SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE);
        assertThat(result.events().getFirst().actorSummary()).contains("Security audit");
        assertThat(result.events().getFirst().statusSummary()).containsIgnoringCase("failure");
    }

    @Test
    void templateAuthorCannotQuerySecurityEvents() {
        ManagementSessionClaims templateAuthor = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        when(groupAccessService.canReadAudit(templateAuthor)).thenReturn(false);

        assertThatThrownBy(() -> service.queryManagementEvents(
                templateAuthor,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        )).isInstanceOf(AuditAccessDeniedException.class);
        verify(managementAuditEventRepository, never()).searchPaged(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        );
    }

    @Test
    void auditAdminCanQueryRetentionPurgeEvidence() {
        ManagementSessionClaims auditAdmin = session("10000004", List.of("AUDIT_ADMIN"), List.of("*"));
        when(groupAccessService.canReadAudit(auditAdmin)).thenReturn(true);
        ManagementAuditEventEntity purge = retentionPurgeEvent();
        when(managementAuditEventRepository.searchPaged(
                isNull(),
                eq(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(purge), 1, 1));

        var result = service.queryManagementEvents(
                auditAdmin,
                AuditReadActorRole.AUDIT_ADMIN,
                null,
                AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType())
                .isEqualTo(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE);
        assertThat(result.events().getFirst().templateId()).isNull();
        assertThat(result.events().getFirst().statusSummary()).contains("deletedCount=");
    }

    @Test
    void groupAdminQueryScopesToGroup_soPlatformPurgeRowsAreExcluded() {
        when(groupAccessService.canReadAudit(groupAdmin)).thenReturn(true);
        TemplateEntity template = templateEntity("RETAIL");
        when(templateService.requireReadableTemplate(TEMPLATE_ID, groupAdmin)).thenReturn(template);
        when(managementAuditEventRepository.searchPaged(
                eq(TEMPLATE_ID),
                eq(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE),
                isNull(),
                isNull(),
                isNull(),
                eq("RETAIL"),
                eq(0),
                eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(), 0, 0));

        var result = service.queryManagementEvents(
                groupAdmin,
                AuditReadActorRole.GROUP_ADMIN,
                TEMPLATE_ID,
                AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE,
                null,
                null,
                null,
                "RETAIL",
                null,
                0,
                20
        );

        assertThat(result.events()).isEmpty();
        verify(managementAuditEventRepository).searchPaged(
                eq(TEMPLATE_ID),
                eq(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE),
                isNull(),
                isNull(),
                isNull(),
                eq("RETAIL"),
                eq(0),
                eq(20)
        );
    }

    @Test
    void exportManagementEventsUsesMaskedExportFormat() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);
        when(managementAuditEventRepository.search(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(sampleManagementEvent()));

        var result = service.exportManagementEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(result.format()).isEqualTo(AuditQueryService.EXPORT_FORMAT);
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().actorSummaryMasked()).contains("****");
    }

    @Test
    void managementEventsEnrichTemplateDisplayFieldsInBatch() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);
        ManagementAuditEventEntity entity = sampleManagementEvent();
        when(managementAuditEventRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 1, 1));
        when(templateService.lookupDisplayInfoByIds(Set.of(TEMPLATE_ID))).thenReturn(Map.of(
                TEMPLATE_ID,
                new TemplateDisplayInfo("Sample", "TPL-001")
        ));

        var result = service.queryManagementEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(result.events().getFirst().templateId()).isEqualTo(TEMPLATE_ID.toString());
        assertThat(result.events().getFirst().templateDisplayName()).isEqualTo("Sample");
        assertThat(result.events().getFirst().templateExternalId()).isEqualTo("TPL-001");
        verify(templateService).lookupDisplayInfoByIds(Set.of(TEMPLATE_ID));
    }

    @Test
    void lifecycleEventsEnrichTemplateAndActorDisplayFieldsInBatch() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);
        TemplateLifecycleRecordEntity record = new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                TEMPLATE_ID,
                LifecycleAction.SUBMIT_FOR_TEST,
                TemplateLifecycleStatus.DRAFT,
                TemplateLifecycleStatus.TESTING,
                null,
                "Ready for test",
                null,
                "10000003"
        );
        when(lifecycleRecordRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(record), 1, 1));
        when(templateService.lookupDisplayInfoByIds(Set.of(TEMPLATE_ID))).thenReturn(Map.of(
                TEMPLATE_ID,
                new TemplateDisplayInfo("Sample", "TPL-001")
        ));
        when(managementUserDisplayService.lookupDisplayNames(Set.of("10000003")))
                .thenReturn(Map.of("10000003", "Lifecycle Tester (10000003)"));

        var result = service.queryLifecycleEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

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
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);
        TemplateLifecycleRecordEntity record = new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                TEMPLATE_ID,
                LifecycleAction.SUBMIT_FOR_TEST,
                TemplateLifecycleStatus.DRAFT,
                TemplateLifecycleStatus.TESTING,
                null,
                "Ready for test",
                null,
                "10000003"
        );
        when(lifecycleRecordRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(record), 1, 1));

        var result = service.queryLifecycleEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType()).isEqualTo("SUBMIT_FOR_TEST");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void groupAdminLifecycleExportRequiresScopedTemplate() {
        when(groupAccessService.canReadAudit(groupAdmin)).thenReturn(true);
        TemplateEntity template = templateEntity("RETAIL");
        when(templateService.requireReadableTemplate(TEMPLATE_ID, groupAdmin)).thenReturn(template);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(TEMPLATE_ID)).thenReturn(List.of());

        var result = service.exportLifecycleEvents(
                groupAdmin,
                AuditReadActorRole.GROUP_ADMIN,
                TEMPLATE_ID,
                null,
                null,
                null,
                "RETAIL",
                null
        );

        assertThat(result.format()).isEqualTo(AuditQueryService.LIFECYCLE_EXPORT_FORMAT);
        verify(templateService, org.mockito.Mockito.times(2))
                .requireReadableTemplate(TEMPLATE_ID, groupAdmin);
    }

    @Test
    void managementEventsFilterByRequestIdQueriesRuntimeAuditStoreWithExactMatch() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);
        RuntimeGenerationAuditEventEntity entity = sampleRuntimeAuditEvent("req-audit-001");
        when(runtimeGenerationAuditEventRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("req-audit-001"), eq(0), eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 1, 1));
        when(templateService.lookupDisplayInfoByIds(Set.of(TEMPLATE_ID))).thenReturn(Map.of(
                TEMPLATE_ID,
                new TemplateDisplayInfo("Sample", "TPL-001")
        ));

        var result = service.queryManagementEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                null,
                "req-audit-001",
                0,
                20
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().requestId()).isEqualTo("req-audit-001");
        assertThat(result.events().getFirst().eventType()).isEqualTo(RuntimeGenerationAuditRecorder.EVENT_SYNC_GENERATION);
        verify(runtimeGenerationAuditEventRepository).searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("req-audit-001"), eq(0), eq(20)
        );
        verify(managementAuditEventRepository, never()).searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)
        );
    }

    @Test
    void lifecycleEventsReturnEmptyWhenRequestIdFilterProvided() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);

        var result = service.queryLifecycleEvents(
                globalAdmin,
                AuditReadActorRole.GLOBAL_ADMIN,
                null,
                null,
                null,
                null,
                null,
                "req-audit-001",
                0,
                20
        );

        assertThat(result.events()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(lifecycleRecordRepository, never()).searchPaged(
                isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)
        );
    }

    private RuntimeGenerationAuditEventEntity sampleRuntimeAuditEvent(String requestId) {
        return new RuntimeGenerationAuditEventEntity(
                UUID.randomUUID(),
                Instant.parse("2026-06-23T10:00:00Z"),
                RuntimeGenerationAuditRecorder.EVENT_SYNC_GENERATION,
                "management",
                TEMPLATE_ID,
                "RETAIL",
                UUID.randomUUID(),
                "fp-CRED-ABCD1234",
                "access-01",
                "1.0.0",
                "1.0.0",
                "EXPLICIT",
                "PDF",
                "ATTACHMENT",
                requestId,
                "hash-idem",
                "ACCEPTED",
                null,
                null,
                "doc-1",
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                "Generated PDF",
                null,
                120L,
                "audit-1",
                "trace-1"
        );
    }

    private ManagementAuditEventEntity sampleManagementEvent() {
        return new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.parse("2026-06-23T10:00:00Z"),
                "API_POLICY_UPDATED",
                TEMPLATE_ID,
                "RETAIL",
                UUID.randomUUID(),
                1,
                2,
                "[\"OUTPUT_POLICY\"]",
                false,
                null,
                "10000002",
                "Group Admin (10000002)",
                "fp-CRED-ABCD1234",
                "Policy updated",
                "[]"
        );
    }

    private ManagementAuditEventEntity retentionPurgeEvent() {
        return new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.parse("2026-07-11T03:00:00Z"),
                AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE,
                null,
                null,
                null,
                null,
                null,
                "[]",
                false,
                null,
                AuditRetentionPurgeEvidenceWriter.ACTOR_USERNAME,
                AuditRetentionPurgeEvidenceWriter.ACTOR_SUMMARY,
                null,
                "table=management_audit_event; retentionDays=90; cutoff=2026-04-12T12:00:00Z; deletedCount=3",
                "[]"
        );
    }

    private ManagementAuditEventEntity securityLoginFailureEvent() {
        return new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.parse("2026-07-11T10:00:00Z"),
                SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE,
                null,
                null,
                null,
                null,
                null,
                "[]",
                false,
                null,
                "10000001",
                "Security audit",
                null,
                "Login failure",
                "[\"trace-1\",\"AUD-1\"]"
        );
    }

    private TemplateEntity templateEntity(String groupCode) {
        TemplateEntity entity = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                groupCode,
                "Sample",
                null,
                UUID.randomUUID(),
                "10000002"
        );
        entity.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        return entity;
    }

    @Test
    void globalAdminQueriesGenerationEventsByExternalId() {
        when(groupAccessService.canReadAudit(globalAdmin)).thenReturn(true);
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID,
                "CORP-FOL-OFFER",
                "CORP",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000002"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        RuntimeGenerationAuditEventEntity runtimeEvent = new RuntimeGenerationAuditEventEntity(
                UUID.randomUUID(),
                Instant.parse("2026-07-09T14:00:00Z"),
                RuntimeGenerationAuditRecorder.EVENT_SYNC_GENERATION,
                "dev",
                TEMPLATE_ID,
                "CORP",
                UUID.randomUUID(),
                "fp-demo",
                "e2e-runtime-caller",
                "1.0.0",
                "1.0.0",
                "DEFAULT_ROUTE",
                "DOCX",
                "SYNC_STREAM",
                "req-demo",
                null,
                null,
                null,
                null,
                "DOC-001",
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                "Generation succeeded",
                null,
                42L,
                "AUD-001",
                "trace-001"
        );
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
        )).thenReturn(new AuditSearchPage<>(List.of(runtimeEvent), 1, 1));

        var result = service.queryGenerationEventsByExternalId(globalAdmin, "CORP-FOL-OFFER", 0, 50);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().templateExternalId()).isEqualTo("CORP-FOL-OFFER");
        assertThat(result.content().getFirst().status()).isEqualTo("SUCCEEDED");
        assertThat(result.content().getFirst().outcome()).isEqualTo("SUCCESS");
        assertThat(result.content().getFirst().accessAccountSummary()).isEqualTo("e2****er");
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                "Test User",
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.audit-console",
                List.of("route.audit-console"),
                Instant.now().plusSeconds(3600)
        );
    }
}
