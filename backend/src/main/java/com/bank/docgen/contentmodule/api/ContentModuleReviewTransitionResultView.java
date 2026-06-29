package com.bank.docgen.contentmodule.api;

public record ContentModuleReviewTransitionResultView(
        boolean applied,
        String errorCode,
        String errorMessage,
        ContentModuleReviewSnapshotView snapshot
) {
}
