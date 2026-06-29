package com.bank.docgen.audit.api;

public record ContentModuleLifecycleAuditDetail(
        int referenceTemplateCount,
        String referenceTemplateListHint,
        String impactedReleaseVersionsHint,
        boolean defaultRouteAffected,
        String recentCallSummary,
        String remediationHint,
        boolean templateStopRequired,
        boolean releaseStopRequired
) {
}
