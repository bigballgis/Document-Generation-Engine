package com.bank.docgen.template.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateImportSummaryView(
        String resolvedTemplateId,
        int newDevelopmentVersion,
        String importBatchId,
        String bundleFormat,
        Integer materializedClauseCount
) {
    public TemplateImportSummaryView(
            String resolvedTemplateId,
            int newDevelopmentVersion,
            String importBatchId
    ) {
        this(resolvedTemplateId, newDevelopmentVersion, importBatchId, null, null);
    }
}
