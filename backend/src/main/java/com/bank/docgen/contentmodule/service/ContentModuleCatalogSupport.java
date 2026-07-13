package com.bank.docgen.contentmodule.service;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.ContentModuleSummaryView;
import com.bank.docgen.contentmodule.api.ContentModuleVersionView;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepositoryCustom.ContentModuleCatalogFilter;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.Locale;

/**
 * Package-private catalog list + view mapping for content modules.
 */
final class ContentModuleCatalogSupport {

    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessService accessSupport;

    ContentModuleCatalogSupport(
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            GroupAccessService groupAccessService,
            ContentModuleAccessService accessSupport
    ) {
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
    }

    PageView<ContentModuleSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String sort
    ) {
        assertCatalogBrowseAllowed(session);
        int safePage = CatalogPageSupport.normalizePage(page);
        int safeSize = CatalogPageSupport.normalizeSize(size);
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        boolean allGroups = groupCodes.contains("*");
        String groupFilter = CatalogPageSupport.blankToNull(groupCode);
        if (groupFilter != null) {
            groupFilter = groupFilter.toUpperCase(Locale.ROOT);
            if (!groupAccessService.canAccessGroup(session, groupFilter)) {
                return new PageView<>(List.of(), safePage, safeSize, 0, 0);
            }
        }

        List<String> normalizedGroups = allGroups
                ? List.of()
                : groupCodes.stream().map(code -> code.trim().toUpperCase(Locale.ROOT)).toList();
        CatalogSortKey sortKey = CatalogSortKey.parse(sort, CatalogSortKey.MODULE_CODE_ASC);
        ContentModuleCatalogFilter filter = new ContentModuleCatalogFilter(
                normalizedGroups,
                allGroups,
                groupFilter,
                CatalogPageSupport.blankToNull(search),
                sortKey
        );
        CatalogQueryPage<ContentModuleEntity> modulePage =
                moduleRepository.searchCatalog(filter, safePage, safeSize);
        List<ContentModuleSummaryView> content = modulePage.content().stream().map(this::toSummary).toList();
        return new PageView<>(
                content,
                safePage,
                safeSize,
                modulePage.totalElements(),
                modulePage.totalPages()
        );
    }

    List<ContentModuleSummaryView> listByGroup(String groupCode, ManagementSessionClaims session) {
        assertCatalogBrowseAllowed(session);
        if (groupCode == null || groupCode.isBlank()) {
            throw new ContentModuleValidationException("api.error.contentModule.groupCodeRequired");
        }
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new ContentModuleAccessDeniedException();
        }
        return list(session, 0, CatalogPageSupport.MAX_SIZE, null, groupCode, null).content();
    }

    ContentModuleDetailView toDetail(ContentModuleEntity module, ManagementSessionClaims session) {
        List<ContentModuleVersionView> versions = versionRepository
                .findByModuleIdOrderBySemanticVersionDesc(module.getId()).stream()
                .map(version -> toVersionView(version, session))
                .toList();
        return new ContentModuleDetailView(
                accessSupport.publicModuleId(module),
                module.getModuleCode(),
                module.getGroupCode(),
                module.getName(),
                module.getDescription(),
                accessSupport.readSharedGroupCodes(module),
                versions
        );
    }

    void assertCatalogBrowseAllowed(ManagementSessionClaims session) {
        if (!groupAccessService.canBrowseContentModuleCatalog(session)) {
            throw new ContentModuleAccessDeniedException();
        }
    }

    private ContentModuleSummaryView toSummary(ContentModuleEntity module) {
        return new ContentModuleSummaryView(
                accessSupport.publicModuleId(module),
                module.getModuleCode(),
                module.getGroupCode(),
                module.getName(),
                module.getDescription(),
                accessSupport.readSharedGroupCodes(module),
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }

    private ContentModuleVersionView toVersionView(
            ContentModuleVersionEntity version,
            ManagementSessionClaims session
    ) {
        String contentStructureJson = groupAccessService.canViewContentModuleStructure(session)
                ? version.getContentStructureJson()
                : null;
        return new ContentModuleVersionView(
                version.getId().toString(),
                version.getSemanticVersion(),
                version.getReviewState(),
                version.getLifecycleState(),
                version.getChangeDescription(),
                contentStructureJson,
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }
}
