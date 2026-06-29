package com.bank.docgen.contentmodule.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ContentModuleLifecycleImpactSummaryView(
        @Min(0) int referenceTemplateCount,
        @NotBlank String referenceTemplateListHint,
        @NotBlank String impactedReleaseVersionsHint,
        boolean defaultRouteAffected,
        @NotBlank String recentCallSummary,
        @NotBlank String remediationHint,
        boolean templateStopRequired,
        boolean releaseStopRequired
) {
}
