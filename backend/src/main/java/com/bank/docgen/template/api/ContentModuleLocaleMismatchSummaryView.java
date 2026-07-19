package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

/**
 * Publish-gate summary for IBL-E1 CONTENT_MODULE_LOCALE_MISMATCH.
 */
public record ContentModuleLocaleMismatchSummaryView(
        boolean blocking,
        int mismatchedReferences,
        int resolvedReferences,
        List<String> mismatchDetails
) {
    public ContentModuleLocaleMismatchSummaryView {
        mismatchDetails = DefensiveCopies.copyList(mismatchDetails);
    }
}
