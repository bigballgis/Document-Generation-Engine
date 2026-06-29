package com.bank.docgen.contentmodule.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleImpactSummaryView;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentModuleLifecycleImpactService {

    private static final List<String> GENERATION_EVENT_TYPES = List.of(
            RuntimeGenerationAuditRecorder.EVENT_SYNC_GENERATION,
            RuntimeGenerationAuditRecorder.EVENT_BATCH_SYNC
    );

    private final ContentModuleAccessSupport accessSupport;
    private final GroupAccessService groupAccessService;
    private final ContentModuleVersionRepository versionRepository;
    private final TemplateContentModuleReferenceRepository referenceRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateRepository templateRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final RuntimeGenerationAuditEventRepository runtimeAuditRepository;

    public ContentModuleLifecycleImpactService(
            ContentModuleAccessSupport accessSupport,
            GroupAccessService groupAccessService,
            ContentModuleVersionRepository versionRepository,
            TemplateContentModuleReferenceRepository referenceRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateRepository templateRepository,
            ApiPolicyRepository apiPolicyRepository,
            RuntimeGenerationAuditEventRepository runtimeAuditRepository
    ) {
        this.accessSupport = accessSupport;
        this.groupAccessService = groupAccessService;
        this.versionRepository = versionRepository;
        this.referenceRepository = referenceRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.templateRepository = templateRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.runtimeAuditRepository = runtimeAuditRepository;
    }

    @Transactional(readOnly = true)
    public ContentModuleLifecycleImpactSummaryView previewImpact(
            String moduleId,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canBrowseContentModuleCatalog(session)) {
            throw new ContentModuleAccessDeniedException();
        }
        ContentModuleEntity module = accessSupport.requireReadableModule(moduleId, session);
        List<UUID> versionIds = versionRepository.findByModuleIdOrderBySemanticVersionDesc(module.getId()).stream()
                .map(ContentModuleVersionEntity::getId)
                .toList();
        if (versionIds.isEmpty()) {
            return emptyImpact();
        }

        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByContentModuleVersionIdIn(versionIds);
        Set<String> templateExternalIds = new LinkedHashSet<>();
        Set<UUID> publishedTemplateIds = new LinkedHashSet<>();
        Set<String> releaseVersions = new LinkedHashSet<>();
        boolean defaultRouteAffected = false;

        for (TemplateContentModuleReferenceEntity reference : references) {
            TemplateVersionEntity templateVersion = templateVersionRepository
                    .findById(reference.getTemplateVersionId())
                    .orElse(null);
            if (templateVersion == null) {
                continue;
            }
            TemplateEntity template = templateRepository
                    .findByIdAndDeletedAtIsNull(templateVersion.getTemplateId())
                    .orElse(null);
            if (template == null) {
                continue;
            }
            templateExternalIds.add(template.getExternalId());
            if (template.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED) {
                publishedTemplateIds.add(template.getId());
            }
            if (templateVersion.getReleaseVersion() != null && !templateVersion.getReleaseVersion().isBlank()) {
                releaseVersions.add(templateVersion.getReleaseVersion());
            }
            if (template.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                    && isDefaultRoute(template.getId(), templateVersion.getReleaseVersion())) {
                defaultRouteAffected = true;
            }
        }

        long recentCallCount = countRecentGenerationCalls(publishedTemplateIds);
        String recentCallSummary = "recentCalls=" + recentCallCount + "/7d";
        boolean blockingCondition = recentCallCount > 0 || defaultRouteAffected;
        boolean templateStopRequired = !publishedTemplateIds.isEmpty() && blockingCondition;
        boolean releaseStopRequired = !releaseVersions.isEmpty() && blockingCondition;

        String remediationHint = versionRepository
                .findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                        module.getId(),
                        com.bank.docgen.contentmodule.domain.ContentModuleReviewState.APPROVED,
                        com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState.ACTIVE
                ).stream()
                .map(ContentModuleVersionEntity::getSemanticVersion)
                .findFirst()
                .map(version -> "upgrade references to " + accessSupport.publicModuleId(module) + " v" + version)
                .orElse("no referencable replacement version available");

        return new ContentModuleLifecycleImpactSummaryView(
                templateExternalIds.size(),
                templateExternalIds.isEmpty() ? "none" : String.join(",", templateExternalIds),
                releaseVersions.isEmpty() ? "none" : String.join(",", releaseVersions),
                defaultRouteAffected,
                recentCallSummary,
                remediationHint,
                templateStopRequired,
                releaseStopRequired
        );
    }

    private long countRecentGenerationCalls(Set<UUID> templateIds) {
        if (templateIds.isEmpty()) {
            return 0L;
        }
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return runtimeAuditRepository.countByTemplateIdInAndEventAtAfterAndEventTypeIn(
                templateIds,
                since,
                GENERATION_EVENT_TYPES
        );
    }

    private boolean isDefaultRoute(UUID templateId, String releaseVersion) {
        if (releaseVersion == null || releaseVersion.isBlank()) {
            return false;
        }
        return apiPolicyRepository.findByTemplateId(templateId)
                .map(ApiPolicyEntity::getDefaultRouteReleaseVersion)
                .map(defaultRoute -> defaultRoute.equals(releaseVersion))
                .orElse(false);
    }

    private ContentModuleLifecycleImpactSummaryView emptyImpact() {
        return new ContentModuleLifecycleImpactSummaryView(
                0,
                "none",
                "none",
                false,
                "recentCalls=0/7d",
                "no referencable replacement version available",
                false,
                false
        );
    }
}
