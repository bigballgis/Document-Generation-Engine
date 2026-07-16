package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.contentmodule.domain.ContentModuleCatalogDisplayStatus;
import java.time.Instant;
import java.util.List;

public interface ContentModuleRepositoryCustom {

    CatalogQueryPage<ContentModuleEntity> searchCatalog(ContentModuleCatalogFilter filter, int page, int size);

    record ContentModuleCatalogFilter(
            List<String> accessibleGroupCodes,
            boolean allGroups,
            String groupCodeExact,
            String search,
            CatalogSortKey sort,
            ContentModuleCatalogDisplayStatus status,
            String jurisdiction,
            String legalReviewRef,
            Instant effectiveFrom,
            Instant effectiveTo
    ) {
        public ContentModuleCatalogFilter(
                List<String> accessibleGroupCodes,
                boolean allGroups,
                String groupCodeExact,
                String search,
                CatalogSortKey sort
        ) {
            this(accessibleGroupCodes, allGroups, groupCodeExact, search, sort, null, null, null, null, null);
        }

        public boolean hasLegalFilters() {
            return jurisdiction != null
                    || legalReviewRef != null
                    || effectiveFrom != null
                    || effectiveTo != null;
        }
    }
}
