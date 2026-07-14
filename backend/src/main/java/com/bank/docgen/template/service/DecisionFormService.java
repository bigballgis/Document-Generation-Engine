package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.domain.LifecycleDecision;
import org.springframework.stereotype.Service;

@Service
public class DecisionFormService {

    public static final String STRUCTURED_OPINION_PREFIX = "[STRUCTURED_OPINION]";
    public static final String EXCEPTION_INTERVENTION_PREFIX = "[EXCEPTION_INTERVENTION]";

    private final GroupAccessService groupAccessService;

    public DecisionFormService(GroupAccessService groupAccessService) {
        this.groupAccessService = groupAccessService;
    }

    public void validateTestDecision(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        validateExceptionIntervention(request, session);
        if (request.decision() == LifecycleDecision.PASSED) {
            requireConfirmed(request.fidelityViewedConfirmed(), "api.error.template.decisionFidelityConfirmationRequired");
            requireConfirmed(request.coverageViewedConfirmed(), "api.error.template.decisionCoverageConfirmationRequired");
            requireConfirmed(request.previewViewedConfirmed(), "api.error.template.decisionPreviewConfirmationRequired");
        } else if (request.decision() == LifecycleDecision.FAILED) {
            requireStructuredNegativeFields(request);
        }
    }

    public void validateApprovalDecision(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        if (request.decision() == LifecycleDecision.APPROVED) {
            if (isBlank(request.commentSummary())) {
                throw new TemplateValidationException("api.error.template.decisionRationaleRequired");
            }
            requireConfirmed(request.fidelityViewedConfirmed(), "api.error.template.decisionFidelityConfirmationRequired");
            requireConfirmed(request.keyEvidenceConfirmed(), "api.error.template.decisionKeyEvidenceConfirmationRequired");
        } else if (request.decision() == LifecycleDecision.REJECTED) {
            requireStructuredNegativeFields(request);
            requireRemediationLinks(request);
        }
    }

    public void validatePublishConfirmation(Boolean fidelityViewedConfirmed) {
        requireConfirmed(fidelityViewedConfirmed, "api.error.template.decisionFidelityConfirmationRequired");
    }

    private void validateExceptionIntervention(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        boolean exceptionRequested = Boolean.TRUE.equals(request.exceptionIntervention());
        if (!exceptionRequested) {
            return;
        }
        if (!session.roles().contains("GROUP_ADMIN")) {
            throw new TemplateValidationException("api.error.template.exceptionInterventionNotAllowed");
        }
        if (isBlank(request.exceptionReason())) {
            throw new TemplateValidationException("api.error.template.exceptionReasonRequired");
        }
        if (!Boolean.TRUE.equals(request.secondaryConfirmed())) {
            throw new TemplateValidationException("api.error.template.exceptionSecondaryConfirmRequired");
        }
    }

    private void requireStructuredNegativeFields(LifecycleDecisionRequest request) {
        if (isBlank(request.reasonCategory())) {
            throw new TemplateValidationException("api.error.template.decisionReasonCategoryRequired");
        }
        if (isBlank(request.impactSummary())) {
            throw new TemplateValidationException("api.error.template.decisionImpactSummaryRequired");
        }
    }

    private void requireRemediationLinks(LifecycleDecisionRequest request) {
        boolean hasLink = !isBlank(request.remediationTestRecordId())
                || !isBlank(request.remediationChangeDiffRef())
                || !isBlank(request.remediationChecklistCode());
        if (!hasLink) {
            throw new TemplateValidationException("api.error.template.decisionRemediationLinkRequired");
        }
    }

    private void requireConfirmed(Boolean flag, String messageKey) {
        if (!Boolean.TRUE.equals(flag)) {
            throw new TemplateValidationException(messageKey);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public boolean isGroupAdminException(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        return Boolean.TRUE.equals(request.exceptionIntervention())
                && (session.roles().contains("GROUP_ADMIN") || session.roles().contains("GLOBAL_ADMIN"));
    }
}
