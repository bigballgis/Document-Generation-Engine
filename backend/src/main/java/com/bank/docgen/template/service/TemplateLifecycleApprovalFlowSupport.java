package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import java.util.UUID;

/**
 * Package-private approval-flow + publish bodies for TemplateLifecycleService.
 */
final class TemplateLifecycleApprovalFlowSupport {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final PublishGateService publishGateService;
    private final DecisionFormService decisionFormService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final CollaborationWorkItemWriter collaborationWorkItemWriter;
    private final RenderProfileService renderProfileService;
    private final ApiPolicyMaterializationService apiPolicyMaterializationService;
    private final VersionFidelityWarningService versionFidelityWarningService;
    private final MessageResolver messageResolver;
    private final TemplateLifecycleTransitionSupport transitions;
    private final TemplateLifecycleDecisionCommentSupport decisionComments;
    private final TemplateLifecycleEligibilitySupport eligibility;

    TemplateLifecycleApprovalFlowSupport(
            TemplateService templateService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            PublishGateService publishGateService,
            DecisionFormService decisionFormService,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            CollaborationWorkItemWriter collaborationWorkItemWriter,
            RenderProfileService renderProfileService,
            ApiPolicyMaterializationService apiPolicyMaterializationService,
            VersionFidelityWarningService versionFidelityWarningService,
            MessageResolver messageResolver,
            TemplateLifecycleTransitionSupport transitions,
            TemplateLifecycleDecisionCommentSupport decisionComments,
            TemplateLifecycleEligibilitySupport eligibility
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.publishGateService = publishGateService;
        this.decisionFormService = decisionFormService;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.collaborationWorkItemWriter = collaborationWorkItemWriter;
        this.renderProfileService = renderProfileService;
        this.apiPolicyMaterializationService = apiPolicyMaterializationService;
        this.versionFidelityWarningService = versionFidelityWarningService;
        this.messageResolver = messageResolver;
        this.transitions = transitions;
        this.decisionComments = decisionComments;
        this.eligibility = eligibility;
    }

    TemplateDetailView submitForApproval(
            UUID templateId,
            LifecycleCommentRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        eligibility.requirePendingSubmitForApproval(template);
        publishGateService.assertReadyForSubmitForApproval(templateId, session);
        transitions.transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.SUBMIT_FOR_APPROVAL,
                null, decisionComments.normalizeComment(request.commentSummary()), session);
        collaborationWorkItemWriter.upsertSubmitForApprovalWorkItem(template, session);
        return templateService.toDetail(template);
    }

    TemplateDetailView recordApprovalDecision(
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

    TemplateDetailView publish(UUID templateId, PublishTemplateRequest request, ManagementSessionClaims session) {
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
}
