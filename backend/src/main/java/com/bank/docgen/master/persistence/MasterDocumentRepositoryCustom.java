package com.bank.docgen.master.persistence;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import java.util.List;
import java.util.Map;

public interface MasterDocumentRepositoryCustom {

    CatalogQueryPage<MasterDocumentEntity> searchCatalog(MasterCatalogFilter filter, int page, int size);

    /**
     * Non-deleted master counts grouped by status within authorized group scope.
     * Empty {@code accessibleGroupCodes} with {@code allGroups=false} yields an empty map.
     */
    Map<MasterDocumentStatus, Long> countGroupedByStatus(List<String> accessibleGroupCodes, boolean allGroups);

    record MasterCatalogFilter(
            List<String> accessibleGroupCodes,
            boolean allGroups,
            String groupCodeExact,
            String search,
            MasterDocumentStatus status,
            CatalogSortKey sort
    ) {
    }
}
