package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
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
class ExceptionInterventionTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private LifecycleImpactPreviewService lifecycleImpactPreviewService;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private PublishGateService publishGateService;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private CollaborationWorkItemWriter collaborationWorkItemWriter;
    @Mock
    private com.bank.docgen.authoring.structured.RenderProfileService renderProfileService;

    private DecisionFormService decisionFormService;
    private TemplateLifecycleService lifecycleService;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims tester;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        decisionFormService = new DecisionFormService(groupAccessService);
        lifecycleService = new TemplateLifecycleService(
                templateService,
                templateRepository,
                templateVersionRepository,
                lifecycleRecordRepository,
                groupAccessService,
                lifecycleImpactPreviewService,
                messageResolver,
                publishGateService,
                decisionFormService,
                contentModuleReferenceService,
                collaborationWorkItemWriter,
                renderProfileService
        );
        templateId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-1",
                "RETAIL",
                "Demo",
                null,
                UUID.randomUUID(),
                "10000002"
        );
        groupAdmin = new ManagementSessionClaims(
                "10000002",
                "Group Admin",
                "group.admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
        tester = new ManagementSessionClaims(
                "10000006",
                "Tester",
                "tester@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_TESTER"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void groupAdminExceptionDecision_requiresReasonAndConfirm() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);

        LifecycleDecisionRequest request = new LifecycleDecisionRequest(
                LifecycleDecision.PASSED,
                "Override",
                null,
                null,
                true,
                true,
                true,
                null,
                null,
                null,
                null,
                true,
                null,
                true
        );

        assertThatThrownBy(() -> lifecycleService.recordTestDecision(templateId, request, groupAdmin))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.exceptionReasonRequired");
    }

    @Test
    void exceptionDecision_writesSeparateAuditMarker() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateRepository.save(any())).thenReturn(template);
        when(templateService.toDetail(template)).thenReturn(null);

        LifecycleDecisionRequest request = new LifecycleDecisionRequest(
                LifecycleDecision.PASSED,
                "Override",
                null,
                null,
                true,
                true,
                true,
                null,
                null,
                null,
                null,
                true,
                "Emergency override approved",
                true
        );

        lifecycleService.recordTestDecision(templateId, request, groupAdmin);

        ArgumentCaptor<TemplateLifecycleRecordEntity> captor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getCommentSummary()).contains(DecisionFormService.EXCEPTION_INTERVENTION_PREFIX);
        assertThat(captor.getValue().getCommentSummary()).contains("Emergency override approved");
    }

    @Test
    void normalTesterDecision_hasNoExceptionMarker() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(tester)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, tester)).thenReturn(template);
        when(templateRepository.save(any())).thenReturn(template);
        when(templateService.toDetail(template)).thenReturn(null);

        LifecycleDecisionRequest request = new LifecycleDecisionRequest(
                LifecycleDecision.PASSED,
                "Looks good",
                null,
                null,
                true,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        lifecycleService.recordTestDecision(templateId, request, tester);

        ArgumentCaptor<TemplateLifecycleRecordEntity> captor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getCommentSummary()).doesNotContain(DecisionFormService.EXCEPTION_INTERVENTION_PREFIX);
    }
}
