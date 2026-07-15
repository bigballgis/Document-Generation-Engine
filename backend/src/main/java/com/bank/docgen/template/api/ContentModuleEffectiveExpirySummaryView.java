package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

/**
 * Publish-gate summary for CE-K08 CONTENT_MODULE_EFFECTIVE_EXPIRED.
 */
public record ContentModuleEffectiveExpirySummaryView(
        boolean blocking,
        int expiredReferences,
        int totalResolvedReferences,
        List<String> expiredDetails
) {
    public ContentModuleEffectiveExpirySummaryView {
        expiredDetails = DefensiveCopies.copyList(expiredDetails);
    }
}
