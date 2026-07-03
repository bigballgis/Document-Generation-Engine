package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewView;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateLifecycleService {

    private static final String STRUCTURED_OPINION_PREFIX = DecisionFormService.STRUCTURED_OPINION_PREFIX;
    private static final ObjectMapper STRUCTURED_OPINION_MAPPER = new ObjectMapper();

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final GroupAccessService groupAccessService;
    private final LifecycleImpactPreviewService lifecycleImpactPreviewService;
    private final MessageResolver messageResolver;
    private final PublishGateService publishGateService;
    private final DecisionFormService decisionFormService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final CollaborationWorkItemWriter collaborationWorkItemWriter;
    private final RenderProfileService renderProfileService;
    private final ApprovalSubStateResolver approvalSubStateResolver;
    private final ApiPolicyRepository apiPolicyRepository;

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
            ApiPolicyRepository apiPolicyRepository
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.groupAccessService = groupAccessService;
        this.lifecycleImpactPreviewService = lifecycleImpactPreviewService;
        this.messageResolver = messageResolver;
        this.publishGateService = publishGateService;
        this.decisionFormService = decisionFormService;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.collaborationWorkItemWriter = collaborationWorkItemWriter;
        this.renderProfileService = renderProfileService;
        this.approvalSubStateResolver = approvalSubStateResolver;
        this.apiPolicyRepository = apiPolicyRepository;
    }

    @Transactional
    public TemplateDetailView submitForTest(UUID templateId, LifecycleCommentRequest request, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        requireResubmitForTestEligible(template);
        transition(template, TemplateLifecycleStatus.TESTING, LifecycleAction.SUBMIT_FOR_TEST, null,
                normalizeComment(request.commentSummary()), session);
        collaborationWorkItemWriter.upsertSubmitForTestWorkItem(template, session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView recordTestDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireTestableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.TESTING);
        decisionFormService.validateTestDecision(request, session);
        String persistedComment = formatDecisionComment(request, session);
        if (request.decision() == LifecycleDecision.PASSED) {
            transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.RECORD_TEST_DECISION,
                    request.decision(), persistedComment, session);
            collaborationWorkItemWriter.resolveOpenTestWorkItems(template, session);
        } else {
            transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_TEST_DECISION,
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
        requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        requirePendingSubmitForApproval(template);
        publishGateService.assertReadyForSubmitForApproval(templateId, session);
        transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.SUBMIT_FOR_APPROVAL,
                null, normalizeComment(request.commentSummary()), session);
        collaborationWorkItemWriter.upsertSubmitForApprovalWorkItem(template, session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView recordApprovalDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireApprovableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        decisionFormService.validateApprovalDecision(request, session);
        String persistedComment = formatDecisionComment(request, session);
        if (request.decision() == LifecycleDecision.APPROVED) {
            transition(template, TemplateLifecycleStatus.PENDING_RELEASE, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session);
            String orchestrator = collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, session)
                    .orElseGet(template::getCreatedBy);
            collaborationWorkItemWriter.upsertPendingReleaseWorkItem(template, orchestrator, session);
        } else {
            transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session);
            String orchestrator = collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, session)
                    .orElseGet(template::getCreatedBy);
            collaborationWorkItemWriter.upsertApprovalFailureRemediationWorkItem(template, orchestrator, session);
        }
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView publish(UUID templateId, PublishTemplateRequest request, ManagementSessionClaims session) {
        TemplateEntity template = requirePublishableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.PENDING_RELEASE);
        publishGateService.assertReady(templateId, session);
        template.setReleaseVersion(request.releaseVersion());
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        TemplateVersionEntity version = requireReleaseCandidateVersion(templateId);
        version.setReleaseVersion(request.releaseVersion());
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        renderProfileService.lockForPublish(version);
        templateVersionRepository.save(version);
        contentModuleReferenceService.lockReferencesForPublish(version.getId());
        recordLifecycle(template, LifecycleAction.PUBLISH, TemplateLifecycleStatus.PENDING_RELEASE,
                TemplateLifecycleStatus.PUBLISHED, null,
                messageResolver.resolve("api.audit.lifecycle.publishedRelease", request.releaseVersion()),
                request.releaseVersion(), session);
        collaborationWorkItemWriter.resolveOpenPendingReleaseWorkItems(template, session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView stop(UUID templateId, LifecycleGovernanceRequest request, ManagementSessionClaims session) {
        requireGovernanceConfirmed(request);
        TemplateEntity template = requireStopEligibleTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.PUBLISHED);
        syncPublishedVersionsToStopped(templateId);
        transition(template, TemplateLifecycleStatus.STOPPED, LifecycleAction.STOP, null, request.reason(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView restore(UUID templateId, LifecycleGovernanceRequest request, ManagementSessionClaims session) {
        requireGovernanceConfirmed(request);
        TemplateEntity template = requireRestoreEligibleTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.STOPPED);
        syncStoppedVersionsToPublished(templateId);
        transition(template, TemplateLifecycleStatus.PUBLISHED, LifecycleAction.RESTORE, null, request.reason(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView deprecate(UUID templateId, LifecycleGovernanceRequest request, ManagementSessionClaims session) {
        requireGovernanceConfirmed(request);
        TemplateEntity template = requireRestoreEligibleTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.STOPPED);
        if (hasCallableVersions(templateId)) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
        syncAllVersionsToDeprecated(templateId);
        transition(template, TemplateLifecycleStatus.DEPRECATED, LifecycleAction.DEPRECATE, null, request.reason(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView deactivateVersion(
            UUID templateId,
            String releaseVersion,
            LifecycleGovernanceRequest request,
            ManagementSessionClaims session
    ) {
        requireGovernanceConfirmed(request);
        TemplateEntity template = requireVersionEligibleTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);
        if (version.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
        apiPolicyRepository.findByTemplateId(templateId).ifPresent(policy -> {
            if (releaseVersion.equals(policy.getDefaultRouteReleaseVersion())) {
                throw new TemplateGovernanceException(
                        ApiErrorCodes.TEMPLATE_DEFAULT_ROUTE_TARGET,
                        "api.error.template.defaultRouteTargetCannotDeactivate",
                        HttpStatus.CONFLICT
                );
            }
        });
        version.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        templateVersionRepository.save(version);
        recordLifecycle(
                template,
                LifecycleAction.DEACTIVATE_VERSION,
                TemplateLifecycleStatus.PUBLISHED,
                TemplateLifecycleStatus.STOPPED,
                null,
                request.reason(),
                releaseVersion,
                session
        );
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView restoreVersion(
            UUID templateId,
            String releaseVersion,
            LifecycleGovernanceRequest request,
            ManagementSessionClaims session
    ) {
        requireGovernanceConfirmed(request);
        TemplateEntity template = requireVersionEligibleTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);
        if (version.getLifecycleStatus() != TemplateLifecycleStatus.STOPPED) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        templateVersionRepository.save(version);
        recordLifecycle(
                template,
                LifecycleAction.RESTORE_VERSION,
                TemplateLifecycleStatus.STOPPED,
                TemplateLifecycleStatus.PUBLISHED,
                null,
                request.reason(),
                releaseVersion,
                session
        );
        return templateService.toDetail(template);
    }

    @Transactional(readOnly = true)
    public LifecycleImpactPreviewView previewImpact(
            UUID templateId,
            LifecycleImpactPreviewRequest request,
            ManagementSessionClaims session
    ) {
        return lifecycleImpactPreviewService.preview(templateId, request, session);
    }

    private void requireGovernanceConfirmed(LifecycleGovernanceRequest request) {
        if (!request.confirmed()) {
            throw new TemplateValidationException("api.error.template.confirmationRequired");
        }
    }

    private String formatDecisionComment(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        String comment = request.commentSummary();
        if (requiresStructuredNegativeOpinion(request.decision())) {
            comment = appendBlock(comment, formatStructuredDecisionComment(request));
        }
        if (request.decision() == LifecycleDecision.REJECTED) {
            comment = appendBlock(comment, formatRemediationLinks(request));
        }
        String exceptionMarker = formatExceptionMarker(request, session);
        if (exceptionMarker != null) {
            comment = appendBlock(comment, exceptionMarker);
        }
        return comment;
    }

    private boolean requiresStructuredNegativeOpinion(LifecycleDecision decision) {
        return decision == LifecycleDecision.FAILED || decision == LifecycleDecision.REJECTED;
    }

    private String formatStructuredDecisionComment(LifecycleDecisionRequest request) {
        Map<String, String> structured = new LinkedHashMap<>();
        structured.put("reasonCategory", request.reasonCategory().trim());
        structured.put("impactSummary", request.impactSummary().trim());
        try {
            return STRUCTURED_OPINION_PREFIX + STRUCTURED_OPINION_MAPPER.writeValueAsString(structured);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private String formatRemediationLinks(LifecycleDecisionRequest request) {
        Map<String, String> remediation = new LinkedHashMap<>();
        if (!isBlank(request.remediationTestRecordId())) {
            remediation.put("testRecordId", request.remediationTestRecordId().trim());
        }
        if (!isBlank(request.remediationChangeDiffRef())) {
            remediation.put("changeDiffRef", request.remediationChangeDiffRef().trim());
        }
        if (!isBlank(request.remediationChecklistCode())) {
            remediation.put("checklistCode", request.remediationChecklistCode().trim());
        }
        if (remediation.isEmpty()) {
            return null;
        }
        try {
            return STRUCTURED_OPINION_PREFIX + STRUCTURED_OPINION_MAPPER.writeValueAsString(remediation);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private String formatExceptionMarker(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        if (!decisionFormService.isGroupAdminException(request, session)) {
            return null;
        }
        Map<String, String> marker = new LinkedHashMap<>();
        marker.put("exceptionReason", request.exceptionReason().trim());
        try {
            return DecisionFormService.EXCEPTION_INTERVENTION_PREFIX
                    + STRUCTURED_OPINION_MAPPER.writeValueAsString(marker);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private String appendBlock(String comment, String block) {
        if (block == null || block.isBlank()) {
            return comment;
        }
        if (comment != null && !comment.isBlank()) {
            return comment.trim() + "\n" + block;
        }
        return block;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeComment(String commentSummary) {
        return commentSummary == null ? "" : commentSummary.trim();
    }

    private void syncPublishedVersionsToStopped(UUID templateId) {
        templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED)
                .forEach(version -> {
                    version.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
                    templateVersionRepository.save(version);
                });
    }

    private void syncStoppedVersionsToPublished(UUID templateId) {
        templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.STOPPED)
                .forEach(version -> {
                    version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
                    templateVersionRepository.save(version);
                });
    }

    private void syncAllVersionsToDeprecated(UUID templateId) {
        templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).forEach(version -> {
            version.setLifecycleStatus(TemplateLifecycleStatus.DEPRECATED);
            templateVersionRepository.save(version);
        });
    }

    private boolean hasCallableVersions(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .anyMatch(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                        && version.getReleaseVersion() != null
                        && !version.getReleaseVersion().isBlank());
    }

    private void transition(
            TemplateEntity template,
            TemplateLifecycleStatus toStatus,
            LifecycleAction action,
            LifecycleDecision decision,
            String comment,
            ManagementSessionClaims session
    ) {
        TemplateLifecycleStatus from = template.getLifecycleStatus();
        template.setLifecycleStatus(toStatus);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        recordLifecycle(template, action, from, toStatus, decision, comment, null, session);
    }

    private void recordLifecycle(
            TemplateEntity template,
            LifecycleAction action,
            TemplateLifecycleStatus from,
            TemplateLifecycleStatus to,
            LifecycleDecision decision,
            String comment,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        lifecycleRecordRepository.save(new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                template.getId(),
                action,
                from,
                to,
                decision,
                comment,
                releaseVersion,
                session.username()
        ));
    }

    private void requireStatus(TemplateEntity template, TemplateLifecycleStatus expected) {
        if (template.getLifecycleStatus() != expected) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    /**
     * Submit-for-test is allowed from DRAFT or from "test passed"
     * (APPROVAL + {@link ApprovalSubState#PENDING_SUBMIT}). Once submitted for approval
     * (APPROVAL + PENDING_DECISION) it is no longer eligible; any other status is rejected (fail-closed).
     */
    private void requireResubmitForTestEligible(TemplateEntity template) {
        TemplateLifecycleStatus status = template.getLifecycleStatus();
        if (status == TemplateLifecycleStatus.DRAFT) {
            return;
        }
        if (status == TemplateLifecycleStatus.APPROVAL
                && approvalSubStateResolver.resolve(template) == ApprovalSubState.PENDING_SUBMIT) {
            return;
        }
        throw new TemplateValidationException("api.error.template.invalidState");
    }

    /**
     * Submit-for-approval requires {@code APPROVAL} + {@link ApprovalSubState#PENDING_SUBMIT}.
     * Once awaiting decision ({@link ApprovalSubState#PENDING_DECISION}) re-submit is rejected (fail-closed).
     */
    private void requirePendingSubmitForApproval(TemplateEntity template) {
        if (approvalSubStateResolver.resolve(template) != ApprovalSubState.PENDING_SUBMIT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    private TemplateEntity requireTestableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateTests(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private TemplateEntity requireApprovableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateApprovals(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private TemplateEntity requirePublishableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canPublishTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private TemplateVersionEntity requireReleaseCandidateVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getReleaseVersion() == null || version.getReleaseVersion().isBlank())
                .findFirst()
                .orElseThrow(TemplateNotFoundException::new);
    }

    private TemplateEntity requireStopEligibleTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canStopTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private TemplateEntity requireRestoreEligibleTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canRestoreOrDeprecateTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private TemplateEntity requireVersionEligibleTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canManageReleaseVersionState(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }
}
