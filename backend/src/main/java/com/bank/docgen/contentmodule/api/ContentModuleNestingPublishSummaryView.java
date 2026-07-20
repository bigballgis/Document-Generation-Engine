package com.bank.docgen.contentmodule.api;

import java.util.List;

/**
 * IBL-E6 publish-gate nesting closure summary (cycle / depth / unpinned).
 */
public record ContentModuleNestingPublishSummaryView(
        boolean cycleBlocking,
        boolean depthBlocking,
        boolean unpinnedBlocking,
        List<String> cycleDetails,
        List<String> depthDetails,
        List<String> unpinnedDetails
) {
    public ContentModuleNestingPublishSummaryView {
        cycleDetails = cycleDetails == null ? List.of() : List.copyOf(cycleDetails);
        depthDetails = depthDetails == null ? List.of() : List.copyOf(depthDetails);
        unpinnedDetails = unpinnedDetails == null ? List.of() : List.copyOf(unpinnedDetails);
    }

    public static ContentModuleNestingPublishSummaryView clear() {
        return new ContentModuleNestingPublishSummaryView(
                false, false, false, List.of(), List.of(), List.of());
    }
}
