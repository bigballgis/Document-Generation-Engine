package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateValidationException;
import java.util.UUID;

/**
 * Package-private access gates for API management (API admin / published template / policy head).
 */
final class ApiManagementAccessSupport {

    private final TemplateService templateService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final GroupAccessService groupAccessService;

    ApiManagementAccessSupport(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            GroupAccessService groupAccessService
    ) {
        this.templateService = templateService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.groupAccessService = groupAccessService;
    }

    TemplateEntity requireApiAdmin(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canManageApiPolicy(session)) {
            throw new ApiManagementAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    TemplateEntity requirePublishedTemplate(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireApiAdmin(templateId, session);
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED
                && template.getLifecycleStatus() != TemplateLifecycleStatus.PENDING_RELEASE) {
            throw new TemplateValidationException("api.error.apimgmt.templateNotPublished");
        }
        return template;
    }

    ApiPolicyEntity requirePolicyHead(UUID templateId, ManagementSessionClaims session) {
        requirePublishedTemplate(templateId, session);
        return apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
    }

    String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }
}
