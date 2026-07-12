package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateRepositoryCustom.TemplateCatalogFilter;
import java.util.List;
import java.util.Locale;

/**
 * Package-private catalog listing and filter parsing for template management.
 */
final class TemplateCatalogSupport {

    private final TemplateRepository templateRepository;
    private final GroupAccessService groupAccessService;
    private final TemplateViewMapper templateViewMapper;
    private final TemplateDisplayEnrichmentSupport displayEnrichment;

    TemplateCatalogSupport(
            TemplateRepository templateRepository,
            GroupAccessService groupAccessService,
            TemplateViewMapper templateViewMapper,
            TemplateDisplayEnrichmentSupport displayEnrichment
    ) {
        this.templateRepository = templateRepository;
        this.groupAccessService = groupAccessService;
        this.templateViewMapper = templateViewMapper;
        this.displayEnrichment = displayEnrichment;
    }

    PageView<TemplateSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String lifecycleStatus,
            String approvalSubState,
            String sort
    ) {
        int safePage = CatalogPageSupport.normalizePage(page);
        int safeSize = CatalogPageSupport.normalizeSize(size);
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        boolean allGroups = groupCodes.contains("*");
        String groupFilter = CatalogPageSupport.blankToNull(groupCode);
        if (groupFilter != null) {
            if (!groupAccessService.canAccessGroup(session, groupFilter)) {
                return new PageView<>(List.of(), safePage, safeSize, 0, 0);
            }
        }

        TemplateLifecycleStatus statusFilter = parseLifecycleStatus(lifecycleStatus);
        if (lifecycleStatus != null && !lifecycleStatus.isBlank() && statusFilter == null) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }
        ApprovalSubState approvalFilter = parseApprovalSubState(approvalSubState);
        if (approvalSubState != null && !approvalSubState.isBlank() && approvalFilter == null) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        CatalogSortKey sortKey = CatalogSortKey.parse(sort, CatalogSortKey.EXTERNAL_ID_ASC);
        TemplateCatalogFilter filter = new TemplateCatalogFilter(
                allGroups ? List.of() : List.copyOf(groupCodes),
                allGroups,
                groupFilter,
                CatalogPageSupport.blankToNull(search),
                statusFilter,
                approvalFilter,
                sortKey
        );
        CatalogQueryPage<TemplateEntity> templatePage = templateRepository.searchCatalog(filter, safePage, safeSize);
        List<TemplateSummaryView> content = displayEnrichment.enrichTemplateSummaries(templatePage.content().stream()
                .map(templateViewMapper::toSummary)
                .toList());
        return new PageView<>(
                content,
                safePage,
                safeSize,
                templatePage.totalElements(),
                templatePage.totalPages()
        );
    }

    List<TemplateSummaryView> listAll(ManagementSessionClaims session) {
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        List<TemplateEntity> templates;
        if (groupCodes.contains("*")) {
            templates = templateRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc();
        } else if (groupCodes.isEmpty()) {
            return List.of();
        } else {
            templates = templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(groupCodes);
        }
        return displayEnrichment.enrichTemplateSummaries(templates.stream().map(templateViewMapper::toSummary).toList());
    }

    private static TemplateLifecycleStatus parseLifecycleStatus(String raw) {
        String value = CatalogPageSupport.blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            return TemplateLifecycleStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ApprovalSubState parseApprovalSubState(String raw) {
        String value = CatalogPageSupport.blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            return ApprovalSubState.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
