package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.LifecycleAuthorizationException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.domain.ApprovalStage;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

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
     * (any decision sub-state) it is no longer eligible; any other status is rejected (fail-closed).
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
     * Once awaiting decision re-submit is rejected (fail-closed).
     */
    void requirePendingSubmitForApproval(TemplateEntity template) {
        if (approvalSubStateResolver.resolve(template) != ApprovalSubState.PENDING_SUBMIT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    ApprovalSubState requireAwaitingApprovalDecision(TemplateEntity template) {
        ApprovalSubState subState = approvalSubStateResolver.resolve(template);
        if (subState != ApprovalSubState.PENDING_DECISION
                && subState != ApprovalSubState.PENDING_LEGAL_DECISION
                && subState != ApprovalSubState.PENDING_COMPLIANCE_DECISION) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
        return subState;
    }

    TemplateEntity requireTestableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateTests(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    /**
     * Load a readable template for approval decision; role gates are applied per stage
     * in {@link TemplateLifecycleApprovalFlowSupport}.
     */
    TemplateEntity requireApprovableTemplate(UUID templateId, ManagementSessionClaims session) {
        return templateService.requireReadableTemplate(templateId, session);
    }

    void requireStageRole(ApprovalStage stage, ManagementSessionClaims session) {
        boolean allowed = stage == ApprovalStage.LEGAL
                ? groupAccessService.canDecideLegalApprovals(session)
                : groupAccessService.canDecideTemplateApprovals(session);
        if (!allowed) {
            throw new LifecycleAuthorizationException(
                    ApiErrorCodes.APPROVAL_STAGE_ROLE_FORBIDDEN,
                    ApiErrorCategories.AUTHORIZATION,
                    "api.error.template.approvalStageRoleForbidden",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    void requireSingleTrackApproverRole(ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateApprovals(session)) {
            throw new LifecycleAuthorizationException(
                    ApiErrorCodes.APPROVAL_STAGE_ROLE_FORBIDDEN,
                    ApiErrorCategories.AUTHORIZATION,
                    "api.error.template.approvalStageRoleForbidden",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    TemplateEntity requirePublishableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canPublishTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    /**
     * FOS-W7-3: publish stamps the same in-flight DEV row the publish gate validated —
     * never a soft-deleted highest {@code dev_version_number}.
     */
    TemplateVersionEntity requireReleaseCandidateVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> !version.isDeleted())
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
