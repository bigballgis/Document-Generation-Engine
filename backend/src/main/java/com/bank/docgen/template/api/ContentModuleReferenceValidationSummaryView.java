package com.bank.docgen.template.api;

public record ContentModuleReferenceValidationSummaryView(
        boolean blocking,
        int totalReferences,
        int invalidReferences
) {
}
