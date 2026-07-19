package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateVersionLineSummaryView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.TemplateVersionLineKind;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Package-private view mapping and display-name enrichment for template version lines.
 */
final class TemplateVersionLineViewSupport {

    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final ApprovalSubStateResolver approvalSubStateResolver;
    private final ManagementUserDisplayService managementUserDisplayService;

    TemplateVersionLineViewSupport(
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            ApprovalSubStateResolver approvalSubStateResolver,
            ManagementUserDisplayService managementUserDisplayService
    ) {
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.approvalSubStateResolver = approvalSubStateResolver;
        this.managementUserDisplayService = managementUserDisplayService;
    }

    List<TemplateVersionLineSummaryView> enrichSummaries(List<TemplateVersionLineSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(TemplateVersionLineSummaryView::updatedBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new TemplateVersionLineSummaryView(
                        summary.devVersionId(),
                        summary.devVersionNumber(),
                        summary.releaseVersion(),
                        summary.lifecycleStatus(),
                        summary.approvalSubState(),
                        summary.lineKind(),
                        summary.updatedAt(),
                        summary.updatedBy(),
                        summary.defaultRouteTarget(),
                        summary.cloneable(),
                        summary.updatedBy() == null ? null : displayNames.get(summary.updatedBy())
                ))
                .toList();
    }

    TemplateVersionLineSummaryView toSummary(
            TemplateVersionEntity version,
            TemplateEntity template,
            String defaultRouteReleaseVersion,
            boolean canAuthor,
            boolean hasInFlight
    ) {
        boolean inFlight = templateCurrentVersionResolver.isInFlight(version);
        TemplateVersionLineKind lineKind = inFlight ? TemplateVersionLineKind.IN_FLIGHT : TemplateVersionLineKind.PUBLISHED;
        Boolean defaultRouteTarget = null;
        if (!inFlight && version.getReleaseVersion() != null) {
            defaultRouteTarget = version.getReleaseVersion().equals(defaultRouteReleaseVersion);
        }
        ApprovalSubState approvalSubState = inFlight ? approvalSubStateResolver.resolve(template) : null;
        boolean cloneable = !inFlight && canAuthor && !hasInFlight;

        return new TemplateVersionLineSummaryView(
                version.getId().toString(),
                version.getDevVersionNumber(),
                version.getReleaseVersion(),
                resolveLifecycleStatus(version, template, inFlight),
                approvalSubState,
                lineKind,
                version.getUpdatedAt(),
                template.getUpdatedBy(),
                defaultRouteTarget,
                cloneable,
                null
        );
    }

    TemplateDetailView overlayReleaseDetailUpdatedBy(
            TemplateDetailView detail,
            TemplateVersionEntity version,
            TemplateExportMasterPinView masterPin
    ) {
        String updatedBy = version.getCreatedBy();
        String updatedByDisplayName = updatedBy == null || updatedBy.isBlank()
                ? null
                : managementUserDisplayService.lookupDisplayNames(Set.of(updatedBy)).get(updatedBy);
        return new TemplateDetailView(
                detail.id(),
                detail.externalId(),
                detail.groupCode(),
                detail.name(),
                detail.description(),
                detail.masterId(),
                detail.lifecycleStatus(),
                detail.approvalSubState(),
                detail.releaseVersion(),
                detail.devVersionId(),
                detail.devVersionNumber(),
                detail.variables(),
                detail.bindings(),
                detail.rules(),
                detail.createdAt(),
                detail.updatedAt(),
                updatedBy,
                updatedByDisplayName,
                detail.readOnly(),
                masterPin,
                detail.nextReviewDue(),
                detail.locale(),
                detail.localeVariantFamilyId(),
                detail.approvalMatrixMode(),
                detail.approvalStage()
        );
    }

    private TemplateLifecycleStatus resolveLifecycleStatus(
            TemplateVersionEntity version,
            TemplateEntity template,
            boolean inFlight
    ) {
        if (inFlight) {
            return template.getLifecycleStatus();
        }
        return version.getLifecycleStatus();
    }
}
