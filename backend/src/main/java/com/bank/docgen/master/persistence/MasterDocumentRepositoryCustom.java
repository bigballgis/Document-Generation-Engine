package com.bank.docgen.master.persistence;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import java.util.List;

public interface MasterDocumentRepositoryCustom {

    CatalogQueryPage<MasterDocumentEntity> searchCatalog(MasterCatalogFilter filter, int page, int size);

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
