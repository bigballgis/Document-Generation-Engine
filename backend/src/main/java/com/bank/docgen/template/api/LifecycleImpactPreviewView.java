package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.LifecycleGovernanceAction;
import java.util.List;

public record LifecycleImpactPreviewView(
        LifecycleGovernanceAction action,
        String releaseVersion,
        List<String> callableReleaseVersions,
        String defaultRouteReleaseVersion,
        boolean defaultRouteImpacted,
        String summaryMessageKey
) {
}
