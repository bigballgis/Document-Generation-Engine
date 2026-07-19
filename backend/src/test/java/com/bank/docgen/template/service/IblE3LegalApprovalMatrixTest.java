package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.LifecycleAuthorizationException;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.ApprovalMatrixMode;
import com.bank.docgen.template.domain.ApprovalStage;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * IBL-E3 / ADR-0064 — multi-stage LEGAL→COMPLIANCE approval matrix (BDD-IBL-E3-001…013,017).
 */
@ExtendWith(MockitoExtension.class)
class IblE3LegalApprovalMatrixTest {

    @Mock private TemplateService templateService;
    @Mock private TemplateRepository templateRepository;
    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock private GroupAccessService groupAccessService;
    @Mock private LifecycleImpactPreviewService lifecycleImpactPreviewService;
    @Mock private MessageResolver messageResolver;
    @Mock private PublishGateService publishGateService;
    @Mock private DecisionFormService decisionFormService;
    @Mock private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock private CollaborationWorkItemWriter collaborationWorkItemWriter;
    @Mock private RenderProfileService renderProfileService;
    @Mock private ApprovalSubStateResolver approvalSubStateResolver;
    @Mock private ApiPolicyMaterializationService apiPolicyMaterializationService;
    @Mock private ApiPolicyRepository apiPolicyRepository;
    @Mock private VersionFidelityWarningService versionFidelityWarningService;
    @Mock private MasterDocumentRepository masterDocumentRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private ObjectStoragePort objectStoragePort;

    private TemplateLifecycleService service;
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
        templateId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId, "TPL-E3", "RETAIL", "Multi-stage", null, UUID.randomUUID(), "10000003");
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        template.setApprovalMatrixMode(ApprovalMatrixMode.LEGAL_THEN_COMPLIANCE);
    }

    @Test
    void bdd004_submitMultiStage_createsLegalQueueNotPendingRelease() {
        ManagementSessionClaims author = session("10000003", List.of("TEMPLATE_AUTHOR"));
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_SUBMIT);
        when(templateService.toDetail(template)).thenReturn(detail(ApprovalSubState.PENDING_LEGAL_DECISION));

        service.submitForApproval(templateId, new LifecycleCommentRequest("Ready"), author);

        verify(collaborationWorkItemWriter).upsertSubmitForLegalReviewWorkItem(template, author);
        verify(collaborationWorkItemWriter, never()).upsertSubmitForApprovalWorkItem(any(), any());
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
    }

    @Test
    void bdd005_legalApprove_advancesToComplianceAndOpensApprovalQueue() {
        ManagementSessionClaims legal = session("10000009", List.of("LEGAL_REVIEWER"));
        when(templateService.requireReadableTemplate(templateId, legal)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_LEGAL_DECISION);
        when(groupAccessService.canDecideLegalApprovals(legal)).thenReturn(true);
        when(collaborationWorkItemWriter.resolveOpenLegalWorkItems(template, legal))
                .thenReturn(Optional.of("10000003"));
        when(templateService.toDetail(template)).thenReturn(detail(ApprovalSubState.PENDING_COMPLIANCE_DECISION));
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId)).thenReturn(List.of());

        service.recordApprovalDecision(templateId, decision(LifecycleDecision.APPROVED, ApprovalStage.LEGAL), legal);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
        verify(collaborationWorkItemWriter).resolveOpenLegalWorkItems(template, legal);
        verify(collaborationWorkItemWriter).upsertSubmitForApprovalWorkItem(template, legal);
        verify(collaborationWorkItemWriter, never()).upsertPendingReleaseWorkItem(any(), any(), any());
    }

    @Test
    void bdd006_complianceApprove_reachesPendingRelease() {
        ManagementSessionClaims approver = session("10000007", List.of("TEMPLATE_APPROVER"));
        when(templateService.requireReadableTemplate(templateId, approver)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_COMPLIANCE_DECISION);
        when(groupAccessService.canDecideTemplateApprovals(approver)).thenReturn(true);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, approver))
                .thenReturn(Optional.of("10000003"));
        when(templateService.toDetail(template)).thenReturn(detail(null));
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId)).thenReturn(List.of());

        service.recordApprovalDecision(
                templateId, decision(LifecycleDecision.APPROVED, ApprovalStage.COMPLIANCE), approver);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PENDING_RELEASE);
        verify(collaborationWorkItemWriter).upsertPendingReleaseWorkItem(eq(template), eq("10000003"), eq(approver));
    }

    @Test
    void bdd007_legalReject_returnsToDraft() {
        ManagementSessionClaims legal = session("10000009", List.of("LEGAL_REVIEWER"));
        when(templateService.requireReadableTemplate(templateId, legal)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_LEGAL_DECISION);
        when(groupAccessService.canDecideLegalApprovals(legal)).thenReturn(true);
        when(collaborationWorkItemWriter.resolveOpenLegalWorkItems(template, legal))
                .thenReturn(Optional.of("10000003"));
        when(templateService.toDetail(template)).thenReturn(detail(null));
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId)).thenReturn(List.of());

        service.recordApprovalDecision(templateId, reject(ApprovalStage.LEGAL), legal);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
        verify(collaborationWorkItemWriter).upsertApprovalFailureRemediationWorkItem(
                template, "10000003", legal);
        verify(collaborationWorkItemWriter, never()).upsertSubmitForApprovalWorkItem(any(), any());
    }

    @Test
    void bdd009_templateApproverCannotDecideLegal() {
        ManagementSessionClaims approver = session("10000007", List.of("TEMPLATE_APPROVER"));
        when(templateService.requireReadableTemplate(templateId, approver)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_LEGAL_DECISION);
        when(groupAccessService.canDecideLegalApprovals(approver)).thenReturn(false);

        assertThatThrownBy(() -> service.recordApprovalDecision(
                templateId, decision(LifecycleDecision.APPROVED, ApprovalStage.LEGAL), approver))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.APPROVAL_STAGE_ROLE_FORBIDDEN));
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
    }

    @Test
    void bdd010_legalReviewerCannotDecideCompliance() {
        ManagementSessionClaims legal = session("10000009", List.of("LEGAL_REVIEWER"));
        when(templateService.requireReadableTemplate(templateId, legal)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_COMPLIANCE_DECISION);
        when(groupAccessService.canDecideTemplateApprovals(legal)).thenReturn(false);

        assertThatThrownBy(() -> service.recordApprovalDecision(
                templateId, decision(LifecycleDecision.APPROVED, ApprovalStage.COMPLIANCE), legal))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.APPROVAL_STAGE_ROLE_FORBIDDEN));
    }

    @Test
    void bdd011_wrongStageMismatch_failClosed() {
        ManagementSessionClaims legal = session("10000009", List.of("LEGAL_REVIEWER"));
        when(templateService.requireReadableTemplate(templateId, legal)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_LEGAL_DECISION);

        assertThatThrownBy(() -> service.recordApprovalDecision(
                templateId, decision(LifecycleDecision.APPROVED, ApprovalStage.COMPLIANCE), legal))
                .isInstanceOf(TemplateGovernanceException.class)
                .satisfies(ex -> assertThat(((TemplateGovernanceException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.APPROVAL_STAGE_MISMATCH));
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
    }

    @Test
    void bdd012_selfApprovalBlockedOnLegalStage() {
        ManagementSessionClaims alice = session("alice", List.of("LEGAL_REVIEWER"));
        when(templateService.requireReadableTemplate(templateId, alice)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_LEGAL_DECISION);
        when(groupAccessService.canDecideLegalApprovals(alice)).thenReturn(true);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId)).thenReturn(List.of(
                new com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity(
                        UUID.randomUUID(), templateId,
                        com.bank.docgen.template.domain.LifecycleAction.SUBMIT_FOR_APPROVAL,
                        TemplateLifecycleStatus.APPROVAL, TemplateLifecycleStatus.APPROVAL,
                        null, "submit", null, "alice")
        ));

        assertThatThrownBy(() -> service.recordApprovalDecision(
                templateId, decision(LifecycleDecision.APPROVED, ApprovalStage.LEGAL), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN));
    }

    @Test
    void bdd013_singleTrack_legalReviewerForbidden() {
        template.setApprovalMatrixMode(ApprovalMatrixMode.SINGLE_TRACK);
        ManagementSessionClaims legal = session("10000009", List.of("LEGAL_REVIEWER"));
        when(templateService.requireReadableTemplate(templateId, legal)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_DECISION);
        when(groupAccessService.canDecideTemplateApprovals(legal)).thenReturn(false);

        assertThatThrownBy(() -> service.recordApprovalDecision(
                templateId, decision(LifecycleDecision.APPROVED, null), legal))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.APPROVAL_STAGE_ROLE_FORBIDDEN));
    }

    @Test
    void bdd001_defaultModeIsSingleTrack() {
        TemplateEntity fresh = new TemplateEntity(
                UUID.randomUUID(), "TPL-NEW", "RETAIL", "New", null, UUID.randomUUID(), "10000003");
        assertThat(fresh.getApprovalMatrixMode()).isEqualTo(ApprovalMatrixMode.SINGLE_TRACK);
    }

    private ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username, "Actor", username + "@example.com", AuthSource.LOCAL,
                roles, List.of("RETAIL"), "route.dashboard-home", List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600));
    }

    private LifecycleDecisionRequest decision(LifecycleDecision decision, ApprovalStage stage) {
        return new LifecycleDecisionRequest(
                decision, "Decision summary", "FUNCTIONAL", "Impact",
                true, null, null, true, null, null, null,
                null, null, null, stage);
    }

    private LifecycleDecisionRequest reject(ApprovalStage stage) {
        return new LifecycleDecisionRequest(
                LifecycleDecision.REJECTED, "Rejected", "FUNCTIONAL", "Impact",
                true, null, null, true, "test-1", null, null,
                null, null, null, stage);
    }

    private TemplateDetailView detail(ApprovalSubState subState) {
        return new TemplateDetailView(
                templateId.toString(), "TPL-E3", "RETAIL", "Multi-stage", null,
                UUID.randomUUID().toString(), template.getLifecycleStatus(), subState, null,
                UUID.randomUUID().toString(), 1, List.of(), List.of(), List.of(),
                Instant.now(), Instant.now(), null, null, false, null, null,
                "en", null, template.getApprovalMatrixMode(),
                ApprovalStage.fromSubState(subState));
    }
}
