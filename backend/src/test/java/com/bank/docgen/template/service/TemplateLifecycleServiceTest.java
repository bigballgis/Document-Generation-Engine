package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateLifecycleServiceTest {

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
    private DecisionFormService decisionFormService;
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
    private ManagementSessionClaims author;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
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
                new SelfApprovalGuard(),
                new TemplateAnnualReviewSupport(java.time.Clock.systemUTC())
        );
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
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
                "10000003"
        );
    }

    @Test
    void submitForTest_transitionsToTestingAndCreatesCollaborationWorkItem() {
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.TESTING));

        TemplateDetailView result = service.submitForTest(
                templateId,
                new LifecycleCommentRequest("Ready for test"),
                author
        );

        assertThat(result.lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        verify(collaborationWorkItemWriter).upsertSubmitForTestWorkItem(eq(template), eq(author));
    }

    @Test
    void submitForTest_allowsBlankOptionalComment() {
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.TESTING));

        TemplateDetailView result = service.submitForTest(
                templateId,
                new LifecycleCommentRequest("   "),
                author
        );

        assertThat(result.lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        verify(lifecycleRecordRepository).save(argThat(record -> "".equals(record.getCommentSummary())));
    }

    @Test
    void submitForTest_fromApprovalPendingSubmit_allowsResubmit() {
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_SUBMIT);
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.TESTING));

        TemplateDetailView result = service.submitForTest(
                templateId,
                new LifecycleCommentRequest("Re-run testing"),
                author
        );

        assertThat(result.lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        verify(collaborationWorkItemWriter).upsertSubmitForTestWorkItem(eq(template), eq(author));
    }

    @Test
    void submitForTest_fromApprovalPendingDecision_isRejectedFailClosed() {
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_DECISION);

        assertThatThrownBy(() -> service.submitForTest(
                templateId,
                new LifecycleCommentRequest("Re-run testing"),
                author
        )).isInstanceOf(TemplateValidationException.class);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
        verify(collaborationWorkItemWriter, never()).upsertSubmitForTestWorkItem(any(), any());
    }

    @Test
    void submitForTest_fromTesting_isRejected() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);

        assertThatThrownBy(() -> service.submitForTest(
                templateId,
                new LifecycleCommentRequest("Re-run testing"),
                author
        )).isInstanceOf(TemplateValidationException.class);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        verify(collaborationWorkItemWriter, never()).upsertSubmitForTestWorkItem(any(), any());
    }

    @Test
    void submitForTest_crossGroupDenied_writesNoWorkItem() {
        when(templateService.requireWritableTemplate(templateId, author))
                .thenThrow(new TemplateAccessDeniedException());

        assertThatThrownBy(() -> service.submitForTest(
                templateId,
                new LifecycleCommentRequest("Ready"),
                author
        )).isInstanceOf(TemplateAccessDeniedException.class);

        verify(collaborationWorkItemWriter, never()).upsertSubmitForTestWorkItem(any(), any());
    }

    @Test
    void recordTestDecision_passed_resolvesOpenTestWorkItem() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(author)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.APPROVAL));

        service.recordTestDecision(templateId, decision(LifecycleDecision.PASSED), author);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
        verify(collaborationWorkItemWriter).resolveOpenTestWorkItems(template, author);
        verify(collaborationWorkItemWriter, never()).upsertRemediationWorkItem(any(), any(), any());
    }

    @Test
    void recordTestDecision_failed_resolvesTestAndUpsertsRemediationForOrchestrator() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(author)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(collaborationWorkItemWriter.resolveOpenTestWorkItems(template, author))
                .thenReturn(Optional.of("10000007"));
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.DRAFT));

        service.recordTestDecision(templateId, decision(LifecycleDecision.FAILED), author);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
        verify(collaborationWorkItemWriter).resolveOpenTestWorkItems(template, author);
        verify(collaborationWorkItemWriter).upsertRemediationWorkItem(template, "10000007", author);
    }

    @Test
    void recordTestDecision_failed_noOpenTest_fallsBackToTemplateOwner() {
        template.setLifecycleStatus(TemplateLifecycleStatus.TESTING);
        when(groupAccessService.canDecideTemplateTests(author)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(collaborationWorkItemWriter.resolveOpenTestWorkItems(template, author))
                .thenReturn(Optional.empty());
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.DRAFT));

        service.recordTestDecision(templateId, decision(LifecycleDecision.FAILED), author);

        verify(collaborationWorkItemWriter).upsertRemediationWorkItem(template, "10000003", author);
    }

    @Test
    void recordTestDecision_crossGroupDenied_writesNoWorkItem() {
        when(groupAccessService.canDecideTemplateTests(author)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, author))
                .thenThrow(new TemplateAccessDeniedException());

        assertThatThrownBy(() -> service.recordTestDecision(
                templateId, decision(LifecycleDecision.FAILED), author
        )).isInstanceOf(TemplateAccessDeniedException.class);

        verify(collaborationWorkItemWriter, never()).resolveOpenTestWorkItems(any(), any());
        verify(collaborationWorkItemWriter, never()).upsertRemediationWorkItem(any(), any(), any());
    }

    @Test
    void submitForApproval_createsApprovalCollaborationWorkItem() {
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_SUBMIT);
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.APPROVAL));
        org.mockito.Mockito.doNothing()
                .when(publishGateService).assertReadyForSubmitForApproval(templateId, author);

        service.submitForApproval(templateId, new LifecycleCommentRequest("Ready for approval"), author);

        verify(publishGateService).assertReadyForSubmitForApproval(templateId, author);
        verify(collaborationWorkItemWriter).upsertSubmitForApprovalWorkItem(template, author);
    }

    @Test
    void submitForApproval_fromApprovalPendingDecision_isRejectedFailClosed() {
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_DECISION);

        assertThatThrownBy(() -> service.submitForApproval(
                templateId,
                new LifecycleCommentRequest("Ready for approval"),
                author
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.invalidState");

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
        verify(publishGateService, never()).assertReadyForSubmitForApproval(any(), any());
        verify(collaborationWorkItemWriter, never()).upsertSubmitForApprovalWorkItem(any(), any());
    }

    @Test
    void submitForApproval_blockedWhenSubmitGateNotReady() {
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_SUBMIT);
        org.mockito.Mockito.doThrow(new TemplateValidationException("api.error.template.submitForApprovalGateBlocked"))
                .when(publishGateService).assertReadyForSubmitForApproval(templateId, author);

        assertThatThrownBy(() -> service.submitForApproval(
                templateId,
                new LifecycleCommentRequest("Ready for approval"),
                author
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.submitForApprovalGateBlocked");

        verify(collaborationWorkItemWriter, never()).upsertSubmitForApprovalWorkItem(any(), any());
    }

    @Test
    void recordApprovalDecision_approved_resolvesApprovalAndUpsertsPendingRelease() {
        ManagementSessionClaims approver = new ManagementSessionClaims(
                "10000004",
                "Approver",
                "approver@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_APPROVER"),
                List.of("RETAIL"),
                "route.template-approver-home",
                List.of("route.template-approver-home"),
                Instant.now().plusSeconds(3600)
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(groupAccessService.canDecideTemplateApprovals(approver)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, approver)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_DECISION);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, approver))
                .thenReturn(Optional.of("10000005"));
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.PENDING_RELEASE));

        service.recordApprovalDecision(templateId, decision(LifecycleDecision.APPROVED), approver);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PENDING_RELEASE);
        verify(collaborationWorkItemWriter).resolveOpenApprovalWorkItems(template, approver);
        verify(collaborationWorkItemWriter).upsertPendingReleaseWorkItem(template, "10000005", approver);
        verify(collaborationWorkItemWriter, never()).upsertApprovalFailureRemediationWorkItem(any(), any(), any());
    }

    @Test
    void recordApprovalDecision_rejected_resolvesApprovalAndUpsertsApprovalFailureRemediation() {
        ManagementSessionClaims approver = new ManagementSessionClaims(
                "10000004",
                "Approver",
                "approver@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_APPROVER"),
                List.of("RETAIL"),
                "route.template-approver-home",
                List.of("route.template-approver-home"),
                Instant.now().plusSeconds(3600)
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(groupAccessService.canDecideTemplateApprovals(approver)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, approver)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_DECISION);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, approver))
                .thenReturn(Optional.of("10000005"));
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.DRAFT));

        service.recordApprovalDecision(templateId, decision(LifecycleDecision.REJECTED), approver);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
        verify(collaborationWorkItemWriter).resolveOpenApprovalWorkItems(template, approver);
        verify(collaborationWorkItemWriter).upsertApprovalFailureRemediationWorkItem(template, "10000005", approver);
        verify(collaborationWorkItemWriter, never()).upsertPendingReleaseWorkItem(any(), any(), any());
    }

    @Test
    void recordApprovalDecision_rejected_noOpenApproval_fallsBackToTemplateOwner() {
        ManagementSessionClaims approver = new ManagementSessionClaims(
                "10000004",
                "Approver",
                "approver@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_APPROVER"),
                List.of("RETAIL"),
                "route.template-approver-home",
                List.of("route.template-approver-home"),
                Instant.now().plusSeconds(3600)
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        when(groupAccessService.canDecideTemplateApprovals(approver)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, approver)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_DECISION);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, approver))
                .thenReturn(Optional.empty());
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.DRAFT));

        service.recordApprovalDecision(templateId, decision(LifecycleDecision.REJECTED), approver);

        verify(collaborationWorkItemWriter).upsertApprovalFailureRemediationWorkItem(template, "10000003", approver);
    }

    private LifecycleDecisionRequest decision(LifecycleDecision decision) {
        return new LifecycleDecisionRequest(
                decision,
                "Decision summary",
                "FUNCTIONAL",
                "Impact summary"
        );
    }

    private TemplateDetailView detail(TemplateLifecycleStatus status) {
        return new TemplateDetailView(
                templateId.toString(),
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID().toString(),
                status,
                null,
                null,
                UUID.randomUUID().toString(),
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                null,
                null,
                false, null,
                null);
    }
}
