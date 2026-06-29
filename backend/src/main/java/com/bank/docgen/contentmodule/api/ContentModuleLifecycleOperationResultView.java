package com.bank.docgen.contentmodule.api;

public record ContentModuleLifecycleOperationResultView(
        boolean applied,
        String errorCode,
        String errorMessage,
        ContentModuleLifecycleSnapshotView snapshot,
        ContentModuleLifecycleImpactSummaryView impactSummary
) {
}
