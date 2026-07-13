package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
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
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateLifecycleService {

    private final TemplateService templateService;
    private final LifecycleImpactPreviewService lifecycleImpactPreviewService;
    private final DecisionFormService decisionFormService;
    private final CollaborationWorkItemWriter collaborationWorkItemWriter;
    private final TemplateLifecycleTransitionSupport transitions;
    private final TemplateLifecycleDecisionCommentSupport decisionComments;
    private final TemplateLifecycleEligibilitySupport eligibility;
    private final TemplateLifecycleVersionSupport versionSupport;
    private final TemplateLifecycleApprovalFlowSupport approvalFlow;
    private final GroupAccessService groupAccessService;

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
            ObjectMapper objectMapper,
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort
    ) {
        this.templateService = templateService;
        this.groupAccessService = groupAccessService;
        this.lifecycleImpactPreviewService = lifecycleImpactPreviewService;
        this.decisionFormService = decisionFormService;
        this.collaborationWorkItemWriter = collaborationWorkItemWriter;
        this.transitions = new TemplateLifecycleTransitionSupport(
                templateRepository,
                templateVersionRepository,
                lifecycleRecordRepository
        );
        this.decisionComments = new TemplateLifecycleDecisionCommentSupport(decisionFormService, objectMapper);
        this.eligibility = new TemplateLifecycleEligibilitySupport(
                templateService,
                templateVersionRepository,
                groupAccessService,
                approvalSubStateResolver
        );
        this.versionSupport = new TemplateLifecycleVersionSupport(
                templateVersionRepository,
                apiPolicyRepository,
                transitions,
                eligibility
        );
        this.approvalFlow = new TemplateLifecycleApprovalFlowSupport(
                templateService,
                templateRepository,
                templateVersionRepository,
                publishGateService,
                decisionFormService,
                contentModuleReferenceService,
                collaborationWorkItemWriter,
                renderProfileService,
                apiPolicyMaterializationService,
                versionFidelityWarningService,
                messageResolver,
                transitions,
                decisionComments,
                eligibility,
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                objectMapper
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
        return approvalFlow.submitForApproval(templateId, request, session);
    }

    @Transactional
    public TemplateDetailView recordApprovalDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        return approvalFlow.recordApprovalDecision(templateId, request, session);
    }

    @Transactional
    public TemplateDetailView publish(UUID templateId, PublishTemplateRequest request, ManagementSessionClaims session) {
        return approvalFlow.publish(templateId, request, session);
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
