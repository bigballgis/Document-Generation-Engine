package com.bank.docgen.library.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LibraryExportTemplateEntryView(
        String templateId,
        String status,
        String reasonCode,
        String path
) {
}
