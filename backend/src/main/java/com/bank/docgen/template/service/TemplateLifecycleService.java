package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewView;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateLifecycleService {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final GroupAccessService groupAccessService;
    private final LifecycleImpactPreviewService lifecycleImpactPreviewService;
    private final MessageResolver messageResolver;
    private final PublishGateService publishGateService;
    private final DecisionFormService decisionFormService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final CollaborationWorkItemWriter collaborationWorkItemWriter;
    private final RenderProfileService renderProfileService;
    private final ApiPolicyMaterializationService apiPolicyMaterializationService;
    private final VersionFidelityWarningService versionFidelityWarningService;
    private final TemplateLifecycleTransitionSupport transitions;
    private final TemplateLifecycleDecisionCommentSupport decisionComments;
    private final TemplateLifecycleEligibilitySupport eligibility;
    private final TemplateLifecycleVersionSupport versionSupport;

    public TemplateLifecycleService(
            TemplateService templateService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            GroupAccessService groupAccessService,
            LifecycleImpactPreviewService lifecycleImpactPreviewService,
            MessageResolver messageResolver,
            PublishGateService publishGateService,
            DecisionFormService decisionFormService,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            CollaborationWorkItemWriter collaborationWorkItemWriter,
            RenderProfileService renderProfileService,
            ApprovalSubStateResolver approvalSubStateResolver,
            ApiPolicyMaterializationService apiPolicyMaterializationService,
            ApiPolicyRepository apiPolicyRepository,
            VersionFidelityWarningService versionFidelityWarningService,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.groupAccessService = groupAccessService;
        this.lifecycleImpactPreviewService = lifecycleImpactPreviewService;
        this.messageResolver = messageResolver;
        this.publishGateService = publishGateService;
        this.decisionFormService = decisionFormService;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.collaborationWorkItemWriter = collaborationWorkItemWriter;
        this.renderProfileService = renderProfileService;
        this.apiPolicyMaterializationService = apiPolicyMaterializationService;
        this.versionFidelityWarningService = versionFidelityWarningService;
        this.transitions = new TemplateLifecycleTransitionSupport(
                templateRepository,
                templateVersionRepository,
                lifecycleRecordRepository
        );
        this.decisionComments = new TemplateLifecycleDecisionCommentSupport(decisionFormService, objectMapper);
        this.eligibility = new TemplateLifecycleEligibilitySupport(
                templateService,
                templateVersionRepository,
                this.groupAccessService,
                approvalSubStateResolver
        );
        this.versionSupport = new TemplateLifecycleVersionSupport(
                templateVersionRepository,
                apiPolicyRepository,
                transitions,
                eligibility
        );
    }

    @Transactional
    public TemplateDetailView submitForTest(UUID templateId, LifecycleCommentRequest request, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        eligibility.requireResubmitForTestEligible(template);
        transitions.transition(template, TemplateLifecycleStatus.TESTING, LifecycleAction.SUBMIT_FOR_TEST, null,
                decisionComments.normalizeComment(request.commentSummary()), session);
        collaborationWorkItemWriter.upsertSubmitForTestWorkItem(template, session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView recordTestDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = eligibility.requireTestableTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.TESTING);
        decisionFormService.validateTestDecision(request, session);
        String persistedComment = decisionComments.formatDecisionComment(request, session);
        if (request.decision() == LifecycleDecision.PASSED) {
            transitions.transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.RECORD_TEST_DECISION,
                    request.decision(), persistedComment, session);
            collaborationWorkItemWriter.resolveOpenTestWorkItems(template, session);
        } else {
            transitions.transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_TEST_DECISION,
                    request.decision(), persistedComment, session);
            String orchestrator = collaborationWorkItemWriter.resolveOpenTestWorkItems(template, session)
                    .orElseGet(template::getCreatedBy);
            collaborationWorkItemWriter.upsertRemediationWorkItem(template, orchestrator, session);
        }
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView submitForApproval(UUID templateId, LifecycleCommentRequest request, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        eligibility.requirePendingSubmitForApproval(template);
        publishGateService.assertReadyForSubmitForApproval(templateId, session);
        transitions.transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.SUBMIT_FOR_APPROVAL,
                null, decisionComments.normalizeComment(request.commentSummary()), session);
        collaborationWorkItemWriter.upsertSubmitForApprovalWorkItem(template, session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView recordApprovalDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = eligibility.requireApprovableTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        decisionFormService.validateApprovalDecision(request, session);
        String persistedComment = decisionComments.formatDecisionComment(request, session);
        if (request.decision() == LifecycleDecision.APPROVED) {
            transitions.transition(template, TemplateLifecycleStatus.PENDING_RELEASE, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session);
            apiPolicyMaterializationService.ensureApiPolicySkeleton(templateId, session.username());
            String orchestrator = collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, session)
                    .orElseGet(template::getCreatedBy);
            collaborationWorkItemWriter.upsertPendingReleaseWorkItem(template, orchestrator, session);
        } else {
            transitions.transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session);
            String orchestrator = collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, session)
                    .orElseGet(template::getCreatedBy);
            collaborationWorkItemWriter.upsertApprovalFailureRemediationWorkItem(template, orchestrator, session);
        }
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView publish(UUID templateId, PublishTemplateRequest request, ManagementSessionClaims session) {
        TemplateEntity template = eligibility.requirePublishableTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.PENDING_RELEASE);
        decisionFormService.validatePublishConfirmation(request.fidelityViewedConfirmed());
        apiPolicyMaterializationService.ensureApiPolicyOnPublish(
                templateId, request.releaseVersion(), session.username());
        publishGateService.assertReady(templateId, session);
        template.setReleaseVersion(request.releaseVersion());
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        TemplateVersionEntity version = eligibility.requireReleaseCandidateVersion(templateId);
        version.setReleaseVersion(request.releaseVersion());
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        renderProfileService.lockForPublish(version);
        versionFidelityWarningService.snapshotOnPublish(version, template.getMasterId());
        templateVersionRepository.save(version);
        contentModuleReferenceService.lockReferencesForPublish(version.getId());
        transitions.recordLifecycle(template, LifecycleAction.PUBLISH, TemplateLifecycleStatus.PENDING_RELEASE,
                TemplateLifecycleStatus.PUBLISHED, null,
                messageResolver.resolve("api.audit.lifecycle.publishedRelease", request.releaseVersion()),
                request.releaseVersion(), session);
        collaborationWorkItemWriter.resolveOpenPendingReleaseWorkItems(template, session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView stop(UUID templateId, LifecycleGovernanceRequest request, ManagementSessionClaims session) {
        eligibility.requireGovernanceConfirmed(request);
        TemplateEntity template = eligibility.requireStopEligibleTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.PUBLISHED);
        transitions.syncPublishedVersionsToStopped(templateId);
        transitions.transition(template, TemplateLifecycleStatus.STOPPED, LifecycleAction.STOP, null, request.reason(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView restore(UUID templateId, LifecycleGovernanceRequest request, ManagementSessionClaims session) {
        eligibility.requireGovernanceConfirmed(request);
        TemplateEntity template = eligibility.requireRestoreEligibleTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.STOPPED);
        transitions.syncStoppedVersionsToPublished(templateId);
        transitions.transition(template, TemplateLifecycleStatus.PUBLISHED, LifecycleAction.RESTORE, null, request.reason(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView deprecate(UUID templateId, LifecycleGovernanceRequest request, ManagementSessionClaims session) {
        eligibility.requireGovernanceConfirmed(request);
        TemplateEntity template = eligibility.requireRestoreEligibleTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.STOPPED);
        if (transitions.hasCallableVersions(templateId)) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
        transitions.syncAllVersionsToDeprecated(templateId);
        transitions.transition(template, TemplateLifecycleStatus.DEPRECATED, LifecycleAction.DEPRECATE, null, request.reason(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView deactivateVersion(
            UUID templateId,
            String releaseVersion,
            LifecycleGovernanceRequest request,
            ManagementSessionClaims session
    ) {
        return templateService.toDetail(
                versionSupport.deactivateVersion(templateId, releaseVersion, request, session));
    }

    @Transactional
    public TemplateDetailView restoreVersion(
            UUID templateId,
            String releaseVersion,
            LifecycleGovernanceRequest request,
            ManagementSessionClaims session
    ) {
        return templateService.toDetail(
                versionSupport.restoreVersion(templateId, releaseVersion, request, session));
    }

    @Transactional(readOnly = true)
    public LifecycleImpactPreviewView previewImpact(
            UUID templateId,
            LifecycleImpactPreviewRequest request,
            ManagementSessionClaims session
    ) {
        return lifecycleImpactPreviewService.preview(templateId, request, session);
    }
}
