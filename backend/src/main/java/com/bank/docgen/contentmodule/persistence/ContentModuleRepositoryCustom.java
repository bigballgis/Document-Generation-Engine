package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.contentmodule.domain.ContentModuleCatalogDisplayStatus;
import com.bank.docgen.contentmodule.domain.ContentModuleSearchMode;
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
            Instant effectiveTo,
            ContentModuleSearchMode searchMode,
            String locale
    ) {
        public ContentModuleCatalogFilter {
            if (searchMode == null) {
                searchMode = ContentModuleSearchMode.NAME;
            }
        }

        public ContentModuleCatalogFilter(
                List<String> accessibleGroupCodes,
                boolean allGroups,
                String groupCodeExact,
                String search,
                CatalogSortKey sort
        ) {
            this(
                    accessibleGroupCodes, allGroups, groupCodeExact, search, sort,
                    null, null, null, null, null, ContentModuleSearchMode.NAME, null
            );
        }

        public ContentModuleCatalogFilter(
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
            this(
                    accessibleGroupCodes, allGroups, groupCodeExact, search, sort,
                    status, jurisdiction, legalReviewRef, effectiveFrom, effectiveTo,
                    ContentModuleSearchMode.NAME, null
            );
        }

        public ContentModuleCatalogFilter(
                List<String> accessibleGroupCodes,
                boolean allGroups,
                String groupCodeExact,
                String search,
                CatalogSortKey sort,
                ContentModuleCatalogDisplayStatus status,
                String jurisdiction,
                String legalReviewRef,
                Instant effectiveFrom,
                Instant effectiveTo,
                ContentModuleSearchMode searchMode
        ) {
            this(
                    accessibleGroupCodes, allGroups, groupCodeExact, search, sort,
                    status, jurisdiction, legalReviewRef, effectiveFrom, effectiveTo,
                    searchMode, null
            );
        }

        public boolean hasLegalFilters() {
            return jurisdiction != null
                    || legalReviewRef != null
                    || effectiveFrom != null
                    || effectiveTo != null;
        }

        public boolean isFullTextSearch() {
            return searchMode == ContentModuleSearchMode.FULL_TEXT
                    && search != null
                    && !search.isBlank();
        }
    }
}
