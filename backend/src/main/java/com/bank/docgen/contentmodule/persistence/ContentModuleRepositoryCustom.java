package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import java.util.List;

public interface ContentModuleRepositoryCustom {

    CatalogQueryPage<ContentModuleEntity> searchCatalog(ContentModuleCatalogFilter filter, int page, int size);

    record ContentModuleCatalogFilter(
            List<String> accessibleGroupCodes,
            boolean allGroups,
            String groupCodeExact,
            String search,
            CatalogSortKey sort
    ) {
    }
}
