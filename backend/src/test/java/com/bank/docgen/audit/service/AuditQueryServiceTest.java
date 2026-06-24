package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
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
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private TemplateService templateService;
    @Mock
    private GroupAccessService groupAccessService;

    private AuditQueryService service;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        service = new AuditQueryService(
                managementAuditEventRepository,
                lifecycleRecordRepository,
                templateService,
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
                0,
                20
        );

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().eventType()).isEqualTo("API_POLICY_UPDATED");
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
                0,
                20
        )).isInstanceOf(AuditAccessDeniedException.class);
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
                null
        );

        assertThat(result.format()).isEqualTo(AuditQueryService.EXPORT_FORMAT);
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().actorSummaryMasked()).contains("****");
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
                "RETAIL"
        );

        assertThat(result.format()).isEqualTo(AuditQueryService.LIFECYCLE_EXPORT_FORMAT);
        verify(templateService, org.mockito.Mockito.times(2))
                .requireReadableTemplate(TEMPLATE_ID, groupAdmin);
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
