package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterCurrentRevisionUnavailableException;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.ApprovalMatrixMode;
import com.bank.docgen.template.domain.ApprovalStage;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Package-private approval-flow + publish bodies for TemplateLifecycleService.
 */
final class TemplateLifecycleApprovalFlowSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateLifecycleApprovalFlowSupport.class);

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
    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final ObjectMapper objectMapper;
    private final SelfApprovalGuard selfApprovalGuard;
    private final TemplateAnnualReviewSupport annualReviewSupport;

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
            TemplateLifecycleEligibilitySupport eligibility,
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            ObjectMapper objectMapper,
            SelfApprovalGuard selfApprovalGuard,
            TemplateAnnualReviewSupport annualReviewSupport
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
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.objectMapper = objectMapper;
        this.selfApprovalGuard = selfApprovalGuard;
        this.annualReviewSupport = annualReviewSupport;
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
        if (template.getApprovalMatrixMode() == ApprovalMatrixMode.LEGAL_THEN_COMPLIANCE) {
            collaborationWorkItemWriter.upsertSubmitForLegalReviewWorkItem(template, session);
        } else {
            collaborationWorkItemWriter.upsertSubmitForApprovalWorkItem(template, session);
        }
        return templateService.toDetail(template);
    }

    TemplateDetailView recordApprovalDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = eligibility.requireApprovableTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        ApprovalSubState subState = eligibility.requireAwaitingApprovalDecision(template);
        ApprovalStage effectiveStage = resolveAndValidateStage(template, subState, request.approvalStage());
        if (effectiveStage == null) {
            eligibility.requireSingleTrackApproverRole(session);
        } else {
            eligibility.requireStageRole(effectiveStage, session);
        }
        decisionFormService.validateApprovalDecision(request, session);
        String lastSubmitActor = transitions.latestSubmitForApprovalActor(templateId);
        SelfApprovalGuard.EnforceOutcome outcome = selfApprovalGuard.enforce(new SelfApprovalGuard.EnforceRequest(
                session.username(),
                lastSubmitActor,
                Boolean.TRUE.equals(request.exceptionIntervention()),
                request.exceptionReason(),
                request.secondaryConfirmed(),
                session,
                "api.error.lifecycle.selfApprovalForbidden",
                "api.error.template.exceptionInterventionNotAllowed",
                "api.error.template.exceptionReasonRequired",
                "api.error.template.exceptionSecondaryConfirmRequired"
        ));
        String persistedComment = decisionComments.formatDecisionComment(request, session, effectiveStage);
        if (effectiveStage == ApprovalStage.LEGAL) {
            return recordLegalStageDecision(template, request, session, outcome, persistedComment);
        }
        return recordComplianceOrSingleTrackDecision(template, request, session, outcome, persistedComment);
    }

    private ApprovalStage resolveAndValidateStage(
            TemplateEntity template,
            ApprovalSubState subState,
            ApprovalStage requestedStage
    ) {
        if (template.getApprovalMatrixMode() == ApprovalMatrixMode.LEGAL_THEN_COMPLIANCE) {
            ApprovalStage expected = ApprovalStage.fromSubState(subState);
            if (expected == null) {
                throw new TemplateValidationException("api.error.template.invalidState");
            }
            if (requestedStage != null && requestedStage != expected) {
                throw new TemplateGovernanceException(
                        ApiErrorCodes.APPROVAL_STAGE_MISMATCH,
                        "api.error.template.approvalStageMismatch",
                        HttpStatus.CONFLICT
                );
            }
            return expected;
        }
        if (requestedStage != null) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.APPROVAL_STAGE_MISMATCH,
                    "api.error.template.approvalStageMismatch",
                    HttpStatus.CONFLICT
            );
        }
        return null;
    }

    private TemplateDetailView recordLegalStageDecision(
            TemplateEntity template,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session,
            SelfApprovalGuard.EnforceOutcome outcome,
            String persistedComment
    ) {
        if (request.decision() == LifecycleDecision.APPROVED) {
            transitions.transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session,
                    outcome.selfApprovalException(), outcome.exceptionReason());
            collaborationWorkItemWriter.resolveOpenLegalWorkItems(template, session);
            collaborationWorkItemWriter.upsertSubmitForApprovalWorkItem(template, session);
        } else {
            transitions.transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session,
                    outcome.selfApprovalException(), outcome.exceptionReason());
            String orchestrator = collaborationWorkItemWriter.resolveOpenLegalWorkItems(template, session)
                    .orElseGet(template::getCreatedBy);
            collaborationWorkItemWriter.upsertApprovalFailureRemediationWorkItem(template, orchestrator, session);
        }
        return templateService.toDetail(template);
    }

    private TemplateDetailView recordComplianceOrSingleTrackDecision(
            TemplateEntity template,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session,
            SelfApprovalGuard.EnforceOutcome outcome,
            String persistedComment
    ) {
        if (request.decision() == LifecycleDecision.APPROVED) {
            transitions.transition(template, TemplateLifecycleStatus.PENDING_RELEASE, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session,
                    outcome.selfApprovalException(), outcome.exceptionReason());
            apiPolicyMaterializationService.ensureApiPolicySkeleton(template.getId(), session.username());
            String orchestrator = collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, session)
                    .orElseGet(template::getCreatedBy);
            collaborationWorkItemWriter.upsertPendingReleaseWorkItem(template, orchestrator, session);
        } else {
            transitions.transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), persistedComment, session,
                    outcome.selfApprovalException(), outcome.exceptionReason());
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
        // CE-K01: resolve + pin the current master revision before any state mutation so
        // that a pinning failure (missing revision / unavailable storage) fails closed and
        // the transaction rolls back without leaving a half-published release.
        PinnedMasterSnapshot pinned = resolvePinnedMaster(template);
        template.setReleaseVersion(request.releaseVersion());
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        annualReviewSupport.seedOnEnterPublishedIfAbsent(template);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        TemplateVersionEntity version = eligibility.requireReleaseCandidateVersion(templateId);
        version.setReleaseVersion(request.releaseVersion());
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        renderProfileService.lockForPublish(version);
        versionFidelityWarningService.snapshotOnPublish(version, template.getMasterId());
        version.setMasterRevisionId(pinned.revisionId());
        version.setMasterFileHash(pinned.fileHash());
        version.setPinMetadataJson(pinnedMetadataJson(ReleaseBundlePinMetadata.published(
                Instant.now().toString(), session.username())));
        templateVersionRepository.save(version);
        contentModuleReferenceService.lockReferencesForPublish(version.getId());
        transitions.recordLifecycle(template, LifecycleAction.PUBLISH, TemplateLifecycleStatus.PENDING_RELEASE,
                TemplateLifecycleStatus.PUBLISHED, null,
                messageResolver.resolve("api.audit.lifecycle.publishedRelease", request.releaseVersion()),
                request.releaseVersion(), session);
        collaborationWorkItemWriter.resolveOpenPendingReleaseWorkItems(template, session);
        return templateService.toDetail(template);
    }

    private PinnedMasterSnapshot resolvePinnedMaster(TemplateEntity template) {
        UUID masterId = template.getMasterId();
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterCurrentRevisionUnavailableException::new);
        UUID revisionId = master.getCurrentRevisionLineId();
        if (revisionId == null) {
            throw new MasterCurrentRevisionUnavailableException();
        }
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(revisionId, masterId)
                .orElseThrow(MasterCurrentRevisionUnavailableException::new);
        // BDD-CE-K01-015: only an APPROVED (active) current revision may be pinned for a new release.
        // Deactivated / rejected / draft current revisions fail closed and leave the publish unstarted.
        MasterDocumentStatus effectiveStatus = revision.isCurrent()
                ? master.getStatus()
                : revision.getStatusSnapshot();
        if (effectiveStatus != MasterDocumentStatus.APPROVED) {
            LOG.warn("CE-K01 publish pinning refused: current revision {} status={} (master={})",
                    revisionId, effectiveStatus, masterId);
            throw new MasterCurrentRevisionUnavailableException();
        }
        try (InputStream stream = objectStoragePort.get(revision.getStorageKey())) {
            return new PinnedMasterSnapshot(revisionId, sha256Hex(stream.readAllBytes()));
        } catch (IOException | RuntimeException ex) {
            LOG.warn("CE-K01 publish pinning failed for master {}: {}", masterId, ex.getMessage());
            throw new MasterCurrentRevisionUnavailableException();
        }
    }

    private String pinnedMetadataJson(ReleaseBundlePinMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (IOException ex) {
            // Fall back to a minimal literal JSON so the publish transaction can still proceed;
            // the pin itself (revisionId + hash) is already durable on the version row.
            return "{\"pinOrigin\":\"" + metadata.pinOrigin() + "\"}";
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private record PinnedMasterSnapshot(UUID revisionId, String fileHash) {
    }
}
