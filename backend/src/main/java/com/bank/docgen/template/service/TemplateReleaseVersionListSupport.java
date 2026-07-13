package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateReleaseVersionView;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.UUID;

/**
 * Package-private release-version listing for template detail views.
 */
final class TemplateReleaseVersionListSupport {

    private final TemplateVersionRepository templateVersionRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateDisplayEnrichmentSupport displayEnrichment;
    private final TemplateAccessGuardSupport access;

    TemplateReleaseVersionListSupport(
            TemplateVersionRepository templateVersionRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateDisplayEnrichmentSupport displayEnrichment,
            TemplateAccessGuardSupport access
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.displayEnrichment = displayEnrichment;
        this.access = access;
    }

    List<TemplateReleaseVersionView> listReleaseVersions(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = access.requireReadable(templateId, session);
        String defaultRoute = apiPolicyRepository.findByTemplateId(templateId)
                .map(ApiPolicyEntity::getDefaultRouteReleaseVersion)
                .orElse(null);
        return displayEnrichment.enrichReleaseVersions(
                templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(template.getId()).stream()
                        .filter(version -> version.getReleaseVersion() != null && !version.getReleaseVersion().isBlank())
                        .map(version -> new TemplateReleaseVersionView(
                                version.getReleaseVersion(),
                                version.getDevVersionNumber(),
                                version.getLifecycleStatus(),
                                version.getUpdatedAt(),
                                version.getCreatedBy(),
                                null,
                                defaultRoute != null && defaultRoute.equals(version.getReleaseVersion())
                        ))
                        .toList()
        );
    }
}
