package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record MasterStyleCatalogView(
        String catalogVersion,
        List<MasterStyleCatalogEntryView> entries
) {
    public MasterStyleCatalogView {
        entries = DefensiveCopies.copyList(entries);
    }
}
