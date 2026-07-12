package com.bank.docgen.audit.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.api.LifecycleAuditExportResult;
import com.bank.docgen.audit.api.LifecycleAuditQueryResult;
import com.bank.docgen.audit.api.ManagementAuditExportResult;
import com.bank.docgen.audit.api.ManagementAuditQueryResult;
import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateService.TemplateDisplayInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CQ-05 shared fixtures for {@link AuditQueryServiceTest}:
 * role sessions, audit entity samples, and common arrange / query helpers.
 */
@ExtendWith(MockitoExtension.class)
abstract class AuditQueryServiceTestSupport {

    protected static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    protected static final int DEFAULT_PAGE = 0;
    protected static final int DEFAULT_SIZE = 20;

    @Mock
    protected ManagementAuditEventRepository managementAuditEventRepository;
    @Mock
    protected RuntimeGenerationAuditEventRepository runtimeGenerationAuditEventRepository;
    @Mock
    protected TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    protected TemplateService templateService;
    @Mock
    protected GroupAccessService groupAccessService;
    @Mock
    protected ManagementUserDisplayService managementUserDisplayService;

    protected AuditQueryService service;
    protected ManagementSessionClaims globalAdmin;
    protected ManagementSessionClaims groupAdmin;
    protected ManagementSessionClaims auditAdmin;
    protected ManagementSessionClaims templateAuthor;

    @BeforeEach
    void setUpAuditQuerySupport() {
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
        auditAdmin = session("10000004", List.of("AUDIT_ADMIN"), List.of("*"));
        templateAuthor = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
    }

    protected void allowAuditRead(ManagementSessionClaims session) {
        when(groupAccessService.canReadAudit(session)).thenReturn(true);
    }

    protected void denyAuditRead(ManagementSessionClaims session) {
        when(groupAccessService.canReadAudit(session)).thenReturn(false);
    }

    protected void stubRetailTemplateReadable(ManagementSessionClaims session) {
        when(templateService.requireReadableTemplate(TEMPLATE_ID, session)).thenReturn(templateEntity("RETAIL"));
    }

    protected void stubSampleTemplateDisplay() {
        when(templateService.lookupDisplayInfoByIds(Set.of(TEMPLATE_ID))).thenReturn(Map.of(
                TEMPLATE_ID,
                new TemplateDisplayInfo("Sample", "TPL-001")
        ));
    }

    protected void stubUnfilteredManagementPage(ManagementAuditEventEntity entity) {
        when(managementAuditEventRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(DEFAULT_PAGE), eq(DEFAULT_SIZE)
        )).thenReturn(pageOf(entity));
    }

    protected void stubUnfilteredManagementPageByEventType(String eventType, ManagementAuditEventEntity entity) {
        when(managementAuditEventRepository.searchPaged(
                isNull(), eq(eventType), isNull(), isNull(), isNull(), isNull(), eq(DEFAULT_PAGE), eq(DEFAULT_SIZE)
        )).thenReturn(pageOf(entity));
    }

    protected void stubGroupScopedManagementPage(
            String eventType,
            String groupScope,
            AuditSearchPage<ManagementAuditEventEntity> page
    ) {
        when(managementAuditEventRepository.searchPaged(
                eq(TEMPLATE_ID),
                eventType == null ? isNull() : eq(eventType),
                isNull(),
                isNull(),
                isNull(),
                eq(groupScope),
                eq(DEFAULT_PAGE),
                eq(DEFAULT_SIZE)
        )).thenReturn(page);
    }

    protected void stubUnfilteredLifecyclePage(TemplateLifecycleRecordEntity record) {
        when(lifecycleRecordRepository.searchPaged(
                isNull(), isNull(), isNull(), isNull(), eq(DEFAULT_PAGE), eq(DEFAULT_SIZE)
        )).thenReturn(pageOf(record));
    }

    protected static <T> AuditSearchPage<T> pageOf(T entity) {
        return new AuditSearchPage<>(List.of(entity), 1, 1);
    }

    protected static <T> AuditSearchPage<T> emptyPage() {
        return new AuditSearchPage<>(List.of(), 0, 0);
    }

    protected ManagementAuditQueryResult queryManagement(
            ManagementSessionClaims session,
            AuditReadActorRole role
    ) {
        return queryManagement(session, role, null, null, null, null, null, null, null);
    }

    protected ManagementAuditQueryResult queryManagement(
            ManagementSessionClaims session,
            AuditReadActorRole role,
            UUID templateId,
            String eventType,
            String groupScope
    ) {
        return queryManagement(session, role, templateId, eventType, null, null, null, groupScope, null);
    }

    protected ManagementAuditQueryResult queryManagement(
            ManagementSessionClaims session,
            AuditReadActorRole role,
            UUID templateId,
            String eventType,
            Instant from,
            Instant to,
            String groupScope,
            String requestId
    ) {
        return queryManagement(session, role, templateId, eventType, null, from, to, groupScope, requestId);
    }

    protected ManagementAuditQueryResult queryManagement(
            ManagementSessionClaims session,
            AuditReadActorRole role,
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant from,
            Instant to,
            String groupScope,
            String requestId
    ) {
        return service.queryManagementEvents(
                session,
                role,
                templateId,
                eventType,
                credentialId,
                from,
                to,
                groupScope,
                requestId,
                DEFAULT_PAGE,
                DEFAULT_SIZE
        );
    }

    protected ManagementAuditExportResult exportManagement(
            ManagementSessionClaims session,
            AuditReadActorRole role
    ) {
        return service.exportManagementEvents(
                session, role, null, null, null, null, null, null, null
        );
    }

    protected LifecycleAuditQueryResult queryLifecycle(
            ManagementSessionClaims session,
            AuditReadActorRole role
    ) {
        return queryLifecycle(session, role, null, null, null, null, null, null);
    }

    protected LifecycleAuditQueryResult queryLifecycle(
            ManagementSessionClaims session,
            AuditReadActorRole role,
            String requestId
    ) {
        return queryLifecycle(session, role, null, null, null, null, null, requestId);
    }

    protected LifecycleAuditQueryResult queryLifecycle(
            ManagementSessionClaims session,
            AuditReadActorRole role,
            UUID templateId,
            String eventType,
            Instant from,
            Instant to,
            String groupScope,
            String requestId
    ) {
        return service.queryLifecycleEvents(
                session,
                role,
                templateId,
                eventType,
                from,
                to,
                groupScope,
                requestId,
                DEFAULT_PAGE,
                DEFAULT_SIZE
        );
    }

    protected LifecycleAuditExportResult exportLifecycle(
            ManagementSessionClaims session,
            AuditReadActorRole role,
            UUID templateId,
            String groupScope
    ) {
        return service.exportLifecycleEvents(
                session, role, templateId, null, null, null, groupScope, null
        );
    }

    protected RuntimeGenerationAuditEventEntity sampleRuntimeAuditEvent(String requestId) {
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

    protected RuntimeGenerationAuditEventEntity corpGenerationAuditEvent() {
        return new RuntimeGenerationAuditEventEntity(
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
    }

    protected ManagementAuditEventEntity sampleManagementEvent() {
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

    protected ManagementAuditEventEntity retentionPurgeEvent() {
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

    protected ManagementAuditEventEntity securityLoginFailureEvent() {
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

    protected TemplateLifecycleRecordEntity sampleLifecycleRecord() {
        return new TemplateLifecycleRecordEntity(
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
    }

    protected TemplateEntity templateEntity(String groupCode) {
        return templateEntity(groupCode, "TPL-001", "Sample", TemplateLifecycleStatus.DRAFT);
    }

    protected TemplateEntity templateEntity(
            String groupCode,
            String externalId,
            String displayName,
            TemplateLifecycleStatus status
    ) {
        TemplateEntity entity = new TemplateEntity(
                TEMPLATE_ID,
                externalId,
                groupCode,
                displayName,
                null,
                UUID.randomUUID(),
                "10000002"
        );
        entity.setLifecycleStatus(status);
        return entity;
    }

    protected ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
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
