package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.Set;

public record MasterStyleCatalogEntryView(
        String styleKey,
        Set<String> applicableNodeTypes,
        String renderPurpose
) {
    public MasterStyleCatalogEntryView {
        applicableNodeTypes = DefensiveCopies.copySet(applicableNodeTypes);
    }

}
