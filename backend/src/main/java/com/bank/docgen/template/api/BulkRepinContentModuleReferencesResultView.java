package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record BulkRepinContentModuleReferencesResultView(
        String contentModuleId,
        String groupCode,
        String fromSemanticVersion,
        String toSemanticVersion,
        boolean useLatestApproved,
        BulkRepinSummaryView summary,
        List<BulkRepinItemView> items
) {
    public BulkRepinContentModuleReferencesResultView {
        items = DefensiveCopies.copyList(items);
    }
}
