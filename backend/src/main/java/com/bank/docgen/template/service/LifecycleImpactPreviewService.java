package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleImpactPreviewRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewView;
import com.bank.docgen.template.domain.LifecycleGovernanceAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LifecycleImpactPreviewService {

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final ApiPolicyRepository apiPolicyRepository;

    public LifecycleImpactPreviewService(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            ApiPolicyRepository apiPolicyRepository
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.apiPolicyRepository = apiPolicyRepository;
    }

    @Transactional(readOnly = true)
    public LifecycleImpactPreviewView preview(
            UUID templateId,
            LifecycleImpactPreviewRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        List<String> callableVersions = resolveCallableReleaseVersions(template);
        String defaultRoute = apiPolicyRepository.findByTemplateId(templateId)
                .map(ApiPolicyEntity::getDefaultRouteReleaseVersion)
                .orElse(null);
        String targetVersion = resolveTargetReleaseVersion(template, request);
        boolean defaultRouteImpacted = isDefaultRouteImpacted(request.action(), defaultRoute, targetVersion, callableVersions);
        return new LifecycleImpactPreviewView(
                request.action(),
                targetVersion,
                callableVersions,
                defaultRoute,
                defaultRouteImpacted,
                summaryMessageKey(request.action(), defaultRouteImpacted)
        );
    }

    private List<String> resolveCallableReleaseVersions(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            return List.of();
        }
        List<String> callable = new ArrayList<>();
        for (TemplateVersionEntity version : templateVersionRepository
                .findByTemplateIdOrderByDevVersionNumberDesc(template.getId())) {
            if (version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                    && version.getReleaseVersion() != null
                    && !version.getReleaseVersion().isBlank()) {
                callable.add(version.getReleaseVersion());
            }
        }
        return callable;
    }

    private String resolveTargetReleaseVersion(TemplateEntity template, LifecycleImpactPreviewRequest request) {
        if (request.releaseVersion() != null && !request.releaseVersion().isBlank()) {
            return request.releaseVersion();
        }
        return template.getReleaseVersion();
    }

    private boolean isDefaultRouteImpacted(
            LifecycleGovernanceAction action,
            String defaultRoute,
            String targetVersion,
            List<String> callableVersions
    ) {
        if (defaultRoute == null || defaultRoute.isBlank()) {
            return false;
        }
        return switch (action) {
            case STOP, DEPRECATE -> callableVersions.contains(defaultRoute);
            case DEACTIVATE_VERSION -> defaultRoute.equals(targetVersion);
            case RESTORE, RESTORE_VERSION -> false;
        };
    }

    private String summaryMessageKey(LifecycleGovernanceAction action, boolean defaultRouteImpacted) {
        if (defaultRouteImpacted) {
            return "api.template.lifecycleImpact.defaultRouteAffected";
        }
        return switch (action) {
            case STOP -> "api.template.lifecycleImpact.stopSummary";
            case RESTORE -> "api.template.lifecycleImpact.restoreSummary";
            case DEPRECATE -> "api.template.lifecycleImpact.deprecateSummary";
            case DEACTIVATE_VERSION -> "api.template.lifecycleImpact.deactivateVersionSummary";
            case RESTORE_VERSION -> "api.template.lifecycleImpact.restoreVersionSummary";
        };
    }
}
