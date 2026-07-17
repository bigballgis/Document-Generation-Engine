package com.bank.docgen.contentmodule.service;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.ContentModuleReviewRecordView;
import com.bank.docgen.contentmodule.api.ContentModuleSummaryView;
import com.bank.docgen.contentmodule.api.ContentModuleVersionView;
import com.bank.docgen.contentmodule.domain.ContentModuleCatalogDisplayStatus;
import com.bank.docgen.contentmodule.domain.ContentModuleSearchMode;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepositoryCustom.ContentModuleCatalogFilter;
import com.bank.docgen.contentmodule.persistence.ContentModuleReviewRecordEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleReviewRecordRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Package-private catalog list + view mapping for content modules.
 */
final class ContentModuleCatalogSupport {

    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final ContentModuleReviewRecordRepository reviewRecordRepository;
    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessService accessSupport;

    ContentModuleCatalogSupport(
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            ContentModuleReviewRecordRepository reviewRecordRepository,
            GroupAccessService groupAccessService,
            ContentModuleAccessService accessSupport
    ) {
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
    }

    PageView<ContentModuleSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String sort,
            String jurisdiction,
            String legalReviewRef,
            Instant effectiveFrom,
            Instant effectiveTo,
            String status
    ) {
        return list(
                session, page, size, search, groupCode, sort,
                jurisdiction, legalReviewRef, effectiveFrom, effectiveTo, status, null
        );
    }

    PageView<ContentModuleSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String sort,
            String jurisdiction,
            String legalReviewRef,
            Instant effectiveFrom,
            Instant effectiveTo,
            String status,
            String searchMode
    ) {
        assertCatalogBrowseAllowed(session);
        int safePage = CatalogPageSupport.normalizePage(page);
        int safeSize = CatalogPageSupport.normalizeSize(size);
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        ContentModuleSearchMode mode = parseSearchMode(searchMode);
        String normalizedSearch = CatalogPageSupport.blankToNull(search);
        if (normalizedSearch != null && normalizedSearch.length() > 200) {
            throw new ContentModuleValidationException("api.error.contentModule.searchTooLong");
        }

        ContentModuleCatalogDisplayStatus statusFilter = parseDisplayStatus(status);
        if (status != null && !status.isBlank() && statusFilter == null) {
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
        boolean fullTextActive = mode == ContentModuleSearchMode.FULL_TEXT && normalizedSearch != null;
        CatalogSortKey sortKey = fullTextActive && (sort == null || sort.isBlank())
                ? null
                : CatalogSortKey.parse(sort, CatalogSortKey.MODULE_CODE_ASC);
        ContentModuleCatalogFilter filter = new ContentModuleCatalogFilter(
                normalizedGroups,
                allGroups,
                groupFilter,
                normalizedSearch,
                sortKey,
                statusFilter,
                ContentModuleLegalMetadataSupport.normalizeText(jurisdiction),
                ContentModuleLegalMetadataSupport.normalizeText(legalReviewRef),
                effectiveFrom,
                effectiveTo,
                mode
        );
        CatalogQueryPage<ContentModuleEntity> modulePage =
                moduleRepository.searchCatalog(filter, safePage, safeSize);
        List<ContentModuleSummaryView> content = toSummaries(modulePage.content());
        return new PageView<>(
                content,
                safePage,
                safeSize,
                modulePage.totalElements(),
                modulePage.totalPages()
        );
    }

    private static ContentModuleSearchMode parseSearchMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ContentModuleSearchMode.NAME;
        }
        try {
            return ContentModuleSearchMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ContentModuleValidationException("api.error.contentModule.searchModeInvalid");
        }
    }

    List<ContentModuleSummaryView> listByGroup(String groupCode, ManagementSessionClaims session) {
        assertCatalogBrowseAllowed(session);
        if (groupCode == null || groupCode.isBlank()) {
            throw new ContentModuleValidationException("api.error.contentModule.groupCodeRequired");
        }
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new ContentModuleAccessDeniedException();
        }
        return list(session, 0, CatalogPageSupport.MAX_SIZE, null, groupCode, null, null, null, null, null, null)
                .content();
    }

    ContentModuleDetailView toDetail(ContentModuleEntity module, ManagementSessionClaims session) {
        List<ContentModuleVersionView> versions = versionRepository
                .findByModuleIdOrderBySemanticVersionDesc(module.getId()).stream()
                .map(version -> toVersionView(version, session))
                .toList();
        List<ContentModuleReviewRecordView> reviewHistory = reviewRecordRepository
                .findByModuleIdOrderByCreatedAtAsc(module.getId()).stream()
                .map(this::toReviewRecordView)
                .toList();
        return new ContentModuleDetailView(
                accessSupport.publicModuleId(module),
                module.getModuleCode(),
                module.getGroupCode(),
                module.getName(),
                module.getDescription(),
                accessSupport.readSharedGroupCodes(module),
                versions,
                reviewHistory
        );
    }

    void assertCatalogBrowseAllowed(ManagementSessionClaims session) {
        if (!groupAccessService.canBrowseContentModuleCatalog(session)) {
            throw new ContentModuleAccessDeniedException();
        }
    }

    private List<ContentModuleSummaryView> toSummaries(List<ContentModuleEntity> modules) {
        if (modules.isEmpty()) {
            return List.of();
        }
        List<UUID> moduleIds = modules.stream().map(ContentModuleEntity::getId).toList();
        Map<UUID, List<ContentModuleVersionEntity>> versionsByModule = versionRepository
                .findByModuleIdIn(moduleIds).stream()
                .collect(Collectors.groupingBy(ContentModuleVersionEntity::getModuleId));
        return modules.stream()
                .map(module -> toSummary(module, selectHeadVersion(versionsByModule.get(module.getId()))))
                .toList();
    }

    private ContentModuleSummaryView toSummary(
            ContentModuleEntity module,
            ContentModuleVersionEntity headVersion
    ) {
        if (headVersion == null) {
            throw new ContentModuleValidationException("api.error.contentModule.versionRequired");
        }
        String lifecycleState = headVersion.getLifecycleState() == null
                ? null
                : headVersion.getLifecycleState().name();
        return new ContentModuleSummaryView(
                accessSupport.publicModuleId(module),
                module.getModuleCode(),
                module.getGroupCode(),
                module.getName(),
                module.getDescription(),
                accessSupport.readSharedGroupCodes(module),
                headVersion.getReviewState().name(),
                lifecycleState,
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }

    static ContentModuleVersionEntity selectHeadVersion(List<ContentModuleVersionEntity> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        return versions.stream()
                .max(Comparator
                        .comparing(ContentModuleVersionEntity::getUpdatedAt)
                        .thenComparing(ContentModuleVersionEntity::getSemanticVersion))
                .orElse(null);
    }

    private static ContentModuleCatalogDisplayStatus parseDisplayStatus(String raw) {
        String value = CatalogPageSupport.blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            return ContentModuleCatalogDisplayStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
                version.getRejectionReason(),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                version.getJurisdiction(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                version.getLegalReviewRef()
        );
    }

    private ContentModuleReviewRecordView toReviewRecordView(ContentModuleReviewRecordEntity record) {
        return new ContentModuleReviewRecordView(
                record.getAction().name(),
                record.getDecision(),
                record.getChangeSummary(),
                record.getCommentSummary(),
                record.getActorUsername(),
                record.getCreatedAt(),
                record.getSemanticVersion(),
                record.isSelfApprovalException() ? Boolean.TRUE : null,
                record.getExceptionReason()
        );
    }
}
