package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.UUID;

/**
 * Package-private eligibility / status gates for lifecycle actions (fail-closed).
 */
final class TemplateLifecycleEligibilitySupport {

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final GroupAccessService groupAccessService;
    private final ApprovalSubStateResolver approvalSubStateResolver;

    TemplateLifecycleEligibilitySupport(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            GroupAccessService groupAccessService,
            ApprovalSubStateResolver approvalSubStateResolver
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.groupAccessService = groupAccessService;
        this.approvalSubStateResolver = approvalSubStateResolver;
    }

    void requireGovernanceConfirmed(LifecycleGovernanceRequest request) {
        if (!request.confirmed()) {
            throw new TemplateValidationException("api.error.template.confirmationRequired");
        }
    }

    void requireStatus(TemplateEntity template, TemplateLifecycleStatus expected) {
        if (template.getLifecycleStatus() != expected) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    /**
     * Submit-for-test is allowed from DRAFT or from "test passed"
     * (APPROVAL + {@link ApprovalSubState#PENDING_SUBMIT}). Once submitted for approval
     * (APPROVAL + PENDING_DECISION) it is no longer eligible; any other status is rejected (fail-closed).
     */
    void requireResubmitForTestEligible(TemplateEntity template) {
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
    void requirePendingSubmitForApproval(TemplateEntity template) {
        if (approvalSubStateResolver.resolve(template) != ApprovalSubState.PENDING_SUBMIT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    TemplateEntity requireTestableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateTests(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    TemplateEntity requireApprovableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateApprovals(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    TemplateEntity requirePublishableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canPublishTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    TemplateVersionEntity requireReleaseCandidateVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getReleaseVersion() == null || version.getReleaseVersion().isBlank())
                .findFirst()
                .orElseThrow(TemplateNotFoundException::new);
    }

    TemplateEntity requireStopEligibleTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canStopTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    TemplateEntity requireRestoreEligibleTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canRestoreOrDeprecateTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    TemplateEntity requireVersionEligibleTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canManageReleaseVersionState(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }
}
