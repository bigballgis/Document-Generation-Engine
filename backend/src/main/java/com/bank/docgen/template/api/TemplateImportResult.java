package com.bank.docgen.template.api;

public record TemplateImportResult(
        TemplateImportSummaryView importSummary,
        TemplateDetailView template
) {
}
