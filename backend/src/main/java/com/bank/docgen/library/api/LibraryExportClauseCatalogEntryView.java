package com.bank.docgen.library.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LibraryExportClauseCatalogEntryView(
        String moduleCode,
        String semanticVersion,
        String sourceModuleId,
        List<String> sourceTemplateIds,
        String path
) {
    public LibraryExportClauseCatalogEntryView {
        sourceTemplateIds = DefensiveCopies.copyList(sourceTemplateIds);
    }
}
