package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
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
class TemplateLifecyclePublishGateTest {

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
    @Mock
    private ApprovalSubStateResolver approvalSubStateResolver;
    @Mock
    private ApiPolicyMaterializationService apiPolicyMaterializationService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private VersionFidelityWarningService versionFidelityWarningService;
    @Mock
    private com.bank.docgen.master.persistence.MasterDocumentRepository masterDocumentRepository;
    @Mock
    private com.bank.docgen.master.persistence.MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock
    private com.bank.docgen.infrastructure.storage.ObjectStoragePort objectStoragePort;

    private TemplateLifecycleService service;
    private DecisionFormService decisionFormService;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims tester;
    private ManagementSessionClaims approver;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        decisionFormService = new DecisionFormService(groupAccessService);
        service = new TemplateLifecycleService(
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
                renderProfileService,
                approvalSubStateResolver,
                apiPolicyMaterializationService,
                apiPolicyRepository,
                versionFidelityWarningService,
                new ObjectMapper(),
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                new SelfApprovalGuard()
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
        approver = new ManagementSessionClaims(
                "10000007",
                "Approver",
                "approver@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_APPROVER"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
        templateId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000002"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PENDING_RELEASE);
    }

    @Test
    void publishBlockedWhenPublishGateNotReady() {
        when(groupAccessService.canPublishTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        org.mockito.Mockito.doThrow(new TemplateValidationException("api.error.template.publishGateBlocked"))
                .when(publishGateService).assertReady(templateId, groupAdmin);

        assertThatThrownBy(() ->
                        service.publish(templateId, new PublishTemplateRequest("1.0.0", true), groupAdmin))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.publishGateBlocked");
    }

    @Test
    void publishBlockedWhenFidelityViewedNotConfirmed() {
        when(groupAccessService.canPublishTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);

        assertThatThrownBy(() ->
                        service.publish(templateId, new PublishTemplateRequest("1.0.0", false), groupAdmin))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue(
                        "messageKey",
                        "api.error.template.decisionFidelityConfirmationRequired"
                );
    }

    @Test
    void publishBlockedWhenFidelityViewedMissing() {
        when(groupAccessService.canPublishTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);

        assertThatThrownBy(() ->
                        service.publish(templateId, new PublishTemplateRequest("1.0.0"), groupAdmin))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue(
                        "messageKey",
                        "api.error.template.decisionFidelityConfirmationRequired"
                );
    }

    @Test
    void testFailDecisionWithoutReasonCategoryThrowsValidationError() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(tester)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, tester)).thenReturn(template);

        assertThatThrownBy(() -> service.recordTestDecision(
                templateId,
                new LifecycleDecisionRequest(LifecycleDecision.FAILED, "Needs fixes", null, "Binding broken"),
                tester
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.decisionReasonCategoryRequired");
    }

    @Test
    void testFailDecisionWithoutImpactSummaryThrowsValidationError() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(tester)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, tester)).thenReturn(template);

        assertThatThrownBy(() -> service.recordTestDecision(
                templateId,
                new LifecycleDecisionRequest(LifecycleDecision.FAILED, "Needs fixes", "BINDING_ISSUE", null),
                tester
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.decisionImpactSummaryRequired");
    }

    @Test
    void testFailDecisionPersistsStructuredOpinionInLifecycleRecord() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(tester)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, tester)).thenReturn(template);
        when(templateRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(template);
        when(templateService.toDetail(template)).thenReturn(null);

        service.recordTestDecision(
                templateId,
                new LifecycleDecisionRequest(
                        LifecycleDecision.FAILED,
                        "Optional note",
                        "BINDING_ISSUE",
                        "Header binding invalid"
                ),
                tester
        );

        org.mockito.ArgumentCaptor<TemplateLifecycleRecordEntity> recordCaptor =
                org.mockito.ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        org.mockito.Mockito.verify(lifecycleRecordRepository).save(recordCaptor.capture());
        TemplateLifecycleRecordEntity record = recordCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(record.getAction())
                .isEqualTo(com.bank.docgen.template.domain.LifecycleAction.RECORD_TEST_DECISION);
        org.assertj.core.api.Assertions.assertThat(record.getDecision()).isEqualTo(LifecycleDecision.FAILED);
        org.assertj.core.api.Assertions.assertThat(record.getCommentSummary()).contains("[STRUCTURED_OPINION]");
        org.assertj.core.api.Assertions.assertThat(record.getCommentSummary()).contains("BINDING_ISSUE");
        org.assertj.core.api.Assertions.assertThat(record.getCommentSummary()).contains("Header binding invalid");
        org.assertj.core.api.Assertions.assertThat(record.getCommentSummary()).contains("Optional note");
    }

    @Test
    void approvalRejectWithoutReasonCategoryThrowsValidationError() {
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(groupAccessService.canDecideTemplateApprovals(approver)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, approver)).thenReturn(template);

        assertThatThrownBy(() -> service.recordApprovalDecision(
                templateId,
                new LifecycleDecisionRequest(LifecycleDecision.REJECTED, "Not ready", null, "Scope changed"),
                approver
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.decisionReasonCategoryRequired");
    }
}
