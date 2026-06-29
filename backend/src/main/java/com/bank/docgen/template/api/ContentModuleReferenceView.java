package com.bank.docgen.template.api;

public record ContentModuleReferenceView(
        String referenceKey,
        String moduleId,
        String semanticVersion,
        boolean locked
) {
}
