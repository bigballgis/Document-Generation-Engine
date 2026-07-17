package com.bank.docgen.library.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LibraryExportScopeView(
        String selection,
        String groupId,
        List<String> templateIds
) {
    public LibraryExportScopeView {
        templateIds = DefensiveCopies.copyList(templateIds);
    }
}
