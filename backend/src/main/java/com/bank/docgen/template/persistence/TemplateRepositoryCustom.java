package com.bank.docgen.template.persistence;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.util.List;
import java.util.Map;

public interface TemplateRepositoryCustom {

    CatalogQueryPage<TemplateEntity> searchCatalog(TemplateCatalogFilter filter, int page, int size);

    /**
     * Non-deleted template counts grouped by lifecycle status within authorized group scope.
     * Empty {@code accessibleGroupCodes} with {@code allGroups=false} yields an empty map.
     */
    Map<TemplateLifecycleStatus, Long> countGroupedByLifecycleStatus(
            List<String> accessibleGroupCodes,
            boolean allGroups
    );

    record TemplateCatalogFilter(
            List<String> accessibleGroupCodes,
            boolean allGroups,
            String groupCodeExact,
            String search,
            TemplateLifecycleStatus lifecycleStatus,
            ApprovalSubState approvalSubState,
            CatalogSortKey sort,
            String locale
    ) {
        public TemplateCatalogFilter(
                List<String> accessibleGroupCodes,
                boolean allGroups,
                String groupCodeExact,
                String search,
                TemplateLifecycleStatus lifecycleStatus,
                ApprovalSubState approvalSubState,
                CatalogSortKey sort
        ) {
            this(
                    accessibleGroupCodes,
                    allGroups,
                    groupCodeExact,
                    search,
                    lifecycleStatus,
                    approvalSubState,
                    sort,
                    null
            );
        }
    }
}
