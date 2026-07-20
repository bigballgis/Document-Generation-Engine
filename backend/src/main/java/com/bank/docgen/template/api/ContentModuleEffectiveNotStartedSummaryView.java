package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

/**
 * Publish-gate summary for IBL-E5 CONTENT_MODULE_EFFECTIVE_NOT_STARTED.
 */
public record ContentModuleEffectiveNotStartedSummaryView(
        boolean blocking,
        int notStartedReferences,
        int totalResolvedReferences,
        List<String> notStartedDetails
) {
    public ContentModuleEffectiveNotStartedSummaryView {
        notStartedDetails = DefensiveCopies.copyList(notStartedDetails);
    }
}
