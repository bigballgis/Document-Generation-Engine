package com.bank.docgen.apimgmt.api;

import java.util.List;

public record ApiRoutesSummaryView(
        String templateExternalId,
        String defaultRouteReleaseVersion,
        String defaultGeneratePath,
        List<ExplicitRoutePathView> explicitPaths
) {
}
