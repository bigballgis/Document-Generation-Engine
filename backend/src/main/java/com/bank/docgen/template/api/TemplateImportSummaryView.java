package com.bank.docgen.template.api;

public record TemplateImportSummaryView(
        String resolvedTemplateId,
        int newDevelopmentVersion,
        String importBatchId
) {
}
