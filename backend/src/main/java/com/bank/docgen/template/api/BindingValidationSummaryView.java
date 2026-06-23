package com.bank.docgen.template.api;

public record BindingValidationSummaryView(
        boolean blocking,
        int totalBindings,
        int validCount,
        int missingAnchorCount,
        int duplicateBindingCount,
        int incompatibleContentTypeCount
) {
}
