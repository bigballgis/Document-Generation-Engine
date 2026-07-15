package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;

public record TemplateImportDependencyItemView(
        TemplateImportDependencyType dependencyType,
        TemplateImportDependencySeverity severity,
        String code,
        String messageKey,
        String detail
) {
}
