package com.bank.docgen.template.api;

public record TemplateRuleValidationSummaryView(
        boolean blocking,
        int totalRules,
        int validCount,
        int missingVariableCount,
        int missingAnchorCount,
        int invalidBranchReferenceCount,
        int malformedRuleCount
) {
}
