package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.template.api.BindingValidationView;
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
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateLifecycleService {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final GroupAccessService groupAccessService;
    private final LifecycleImpactPreviewService lifecycleImpactPreviewService;
    private final MessageResolver messageResolver;
    private final ApiPolicyRepository apiPolicyRepository;

    public TemplateLifecycleService(
            TemplateService templateService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            GroupAccessService groupAccessService,
            LifecycleImpactPreviewService lifecycleImpactPreviewService,
            MessageResolver messageResolver,
            ApiPolicyRepository apiPolicyRepository
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.groupAccessService = groupAccessService;
        this.lifecycleImpactPreviewService = lifecycleImpactPreviewService;
        this.messageResolver = messageResolver;
        this.apiPolicyRepository = apiPolicyRepository;
    }

    @Transactional
    public TemplateDetailView submitForTest(UUID templateId, LifecycleCommentRequest request, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.DRAFT);
        transition(template, TemplateLifecycleStatus.TESTING, LifecycleAction.SUBMIT_FOR_TEST, null, request.commentSummary(), session);
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
        if (request.decision() == LifecycleDecision.PASSED) {
            transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.RECORD_TEST_DECISION,
                    request.decision(), request.commentSummary(), session);
        } else {
            transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_TEST_DECISION,
                    request.decision(), request.commentSummary(), session);
        }
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView submitForApproval(UUID templateId, LifecycleCommentRequest request, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.SUBMIT_FOR_APPROVAL,
                null, request.commentSummary(), session);
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
        if (request.decision() == LifecycleDecision.APPROVED) {
            transition(template, TemplateLifecycleStatus.PENDING_RELEASE, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), request.commentSummary(), session);
        } else {
            transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), request.commentSummary(), session);
        }
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView publish(UUID templateId, PublishTemplateRequest request, ManagementSessionClaims session) {
        TemplateEntity template = requirePublishableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.PENDING_RELEASE);
        assertPublishGateReady(templateId, session);
        template.setReleaseVersion(request.releaseVersion());
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        TemplateVersionEntity version = requireReleaseCandidateVersion(templateId);
        version.setReleaseVersion(request.releaseVersion());
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        templateVersionRepository.save(version);
        recordLifecycle(template, LifecycleAction.PUBLISH, TemplateLifecycleStatus.PENDING_RELEASE,
                TemplateLifecycleStatus.PUBLISHED, null,
                messageResolver.resolve("api.audit.lifecycle.publishedRelease", request.releaseVersion()),
                request.releaseVersion(), session);
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

    private void assertPublishGateReady(UUID templateId, ManagementSessionClaims session) {
        BindingValidationView bindings = templateService.validateBindings(templateId, session);
        if (bindings.summary().blocking()) {
            throw new TemplateValidationException("api.error.template.publishGateBlocked");
        }
        if (apiPolicyRepository.findByTemplateId(templateId).isEmpty()) {
            throw new TemplateValidationException("api.error.runtime.policyNotConfigured");
        }
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
