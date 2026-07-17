package com.bank.docgen.library.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LibraryExportMasterCatalogEntryView(
        String masterFileHash,
        String masterRevisionId,
        Integer revisionSequence,
        List<String> sourceTemplateIds,
        String path
) {
    public LibraryExportMasterCatalogEntryView {
        sourceTemplateIds = DefensiveCopies.copyList(sourceTemplateIds);
    }
}
