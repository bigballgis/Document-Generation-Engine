package com.bank.docgen.template.api;

public record TemplateImportDryRunResult(
        boolean imported,
        TemplateImportDependencyReportView dependencyReport
) {
}
