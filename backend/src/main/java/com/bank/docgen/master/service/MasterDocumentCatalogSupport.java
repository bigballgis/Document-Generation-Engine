package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.master.api.MasterDocumentSummaryView;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterDocumentRepositoryCustom.MasterCatalogFilter;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private master-document catalog list / status parsing.
 */
final class MasterDocumentCatalogSupport {

    private final MasterDocumentRepository masterDocumentRepository;
    private final GroupAccessService groupAccessService;
    private final MasterDocumentAccessSupport access;
    private final MasterDocumentViewSupport views;

    MasterDocumentCatalogSupport(
            MasterDocumentRepository masterDocumentRepository,
            GroupAccessService groupAccessService,
            MasterDocumentAccessSupport access,
            MasterDocumentViewSupport views
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.groupAccessService = groupAccessService;
        this.access = access;
        this.views = views;
    }

    PageView<MasterDocumentSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String status,
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
        if (groupFilter != null && !access.canAccessGroup(session, groupFilter)) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        MasterDocumentStatus statusFilter = parseStatus(status);
        if (status != null && !status.isBlank() && statusFilter == null) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        CatalogSortKey sortKey = CatalogSortKey.parse(sort);
        MasterCatalogFilter filter = new MasterCatalogFilter(
                allGroups ? List.of() : List.copyOf(groupCodes),
                allGroups,
                groupFilter,
                CatalogPageSupport.blankToNull(search),
                statusFilter,
                sortKey
        );
        CatalogQueryPage<MasterDocumentEntity> masterPage =
                masterDocumentRepository.searchCatalog(filter, safePage, safeSize);
        Map<UUID, Long> anchorCounts = views.loadAnchorCounts(masterPage.content());
        List<MasterDocumentSummaryView> content = views.enrichMasterSummaries(masterPage.content().stream()
                .map(master -> views.toSummary(master, anchorCounts.getOrDefault(master.getId(), 0L)))
                .toList());
        return new PageView<>(
                content,
                safePage,
                safeSize,
                masterPage.totalElements(),
                masterPage.totalPages()
        );
    }

    static MasterDocumentStatus parseStatus(String raw) {
        String value = CatalogPageSupport.blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            return MasterDocumentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
