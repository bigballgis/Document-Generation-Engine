package com.bank.docgen.template.service;

import com.bank.docgen.template.api.TemplateImportDependencyReportView;

/**
 * CE-E01: commit import blocked by unmet dependencies (422 + dependencyReport).
 */
public class TemplateImportDependenciesException extends RuntimeException {

    private final TemplateImportDependencyReportView dependencyReport;

    public TemplateImportDependenciesException(TemplateImportDependencyReportView dependencyReport) {
        super("api.error.template.importDependenciesUnsatisfied");
        this.dependencyReport = dependencyReport;
    }

    public String messageKey() {
        return "api.error.template.importDependenciesUnsatisfied";
    }

    public TemplateImportDependencyReportView dependencyReport() {
        return dependencyReport;
    }
}
