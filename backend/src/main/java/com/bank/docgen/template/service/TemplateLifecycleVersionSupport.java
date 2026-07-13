package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Package-private per-version deactivate / restore governance bodies.
 */
final class TemplateLifecycleVersionSupport {

    private final TemplateVersionRepository templateVersionRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateLifecycleTransitionSupport transitions;
    private final TemplateLifecycleEligibilitySupport eligibility;

    TemplateLifecycleVersionSupport(
            TemplateVersionRepository templateVersionRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateLifecycleTransitionSupport transitions,
            TemplateLifecycleEligibilitySupport eligibility
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.transitions = transitions;
        this.eligibility = eligibility;
    }

    TemplateEntity deactivateVersion(
            UUID templateId,
            String releaseVersion,
            LifecycleGovernanceRequest request,
            ManagementSessionClaims session
    ) {
        eligibility.requireGovernanceConfirmed(request);
        TemplateEntity template = eligibility.requireVersionEligibleTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.PUBLISHED);
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
        transitions.recordLifecycle(
                template,
                LifecycleAction.DEACTIVATE_VERSION,
                TemplateLifecycleStatus.PUBLISHED,
                TemplateLifecycleStatus.STOPPED,
                null,
                request.reason(),
                releaseVersion,
                session
        );
        return template;
    }

    TemplateEntity restoreVersion(
            UUID templateId,
            String releaseVersion,
            LifecycleGovernanceRequest request,
            ManagementSessionClaims session
    ) {
        eligibility.requireGovernanceConfirmed(request);
        TemplateEntity template = eligibility.requireVersionEligibleTemplate(templateId, session);
        eligibility.requireStatus(template, TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);
        if (version.getLifecycleStatus() != TemplateLifecycleStatus.STOPPED) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        templateVersionRepository.save(version);
        transitions.recordLifecycle(
                template,
                LifecycleAction.RESTORE_VERSION,
                TemplateLifecycleStatus.STOPPED,
                TemplateLifecycleStatus.PUBLISHED,
                null,
                request.reason(),
                releaseVersion,
                session
        );
        return template;
    }
}
