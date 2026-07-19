package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.template.api.TemplateReleaseVersionView;
import com.bank.docgen.template.api.TemplateSummaryView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Package-private display-name enrichment for template catalog and release-version views.
 */
final class TemplateDisplayEnrichmentSupport {

    private final ManagementUserDisplayService managementUserDisplayService;

    TemplateDisplayEnrichmentSupport(ManagementUserDisplayService managementUserDisplayService) {
        this.managementUserDisplayService = managementUserDisplayService;
    }

    List<TemplateSummaryView> enrichTemplateSummaries(List<TemplateSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(TemplateSummaryView::updatedBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new TemplateSummaryView(
                        summary.id(),
                        summary.externalId(),
                        summary.groupCode(),
                        summary.name(),
                        summary.lifecycleStatus(),
                        summary.approvalSubState(),
                        summary.releaseVersion(),
                        summary.releaseVersionCount(),
                        summary.masterId(),
                        summary.updatedBy(),
                        summary.updatedAt(),
                        summary.updatedBy() == null ? null : displayNames.get(summary.updatedBy()),
                        summary.nextReviewDue(),
                        summary.locale(),
                        summary.localeVariantFamilyId(),
                        summary.approvalMatrixMode(),
                        summary.approvalStage()
                ))
                .toList();
    }

    List<TemplateReleaseVersionView> enrichReleaseVersions(List<TemplateReleaseVersionView> versions) {
        if (versions.isEmpty()) {
            return versions;
        }
        Set<String> usernames = versions.stream()
                .map(TemplateReleaseVersionView::updatedBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return versions.stream()
                .map(version -> new TemplateReleaseVersionView(
                        version.releaseVersion(),
                        version.devVersionNumber(),
                        version.lifecycleStatus(),
                        version.updatedAt(),
                        version.updatedBy(),
                        version.updatedBy() == null ? null : displayNames.get(version.updatedBy()),
                        version.defaultRouteTarget()
                ))
                .toList();
    }
}
