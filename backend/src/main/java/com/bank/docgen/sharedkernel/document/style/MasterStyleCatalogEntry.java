package com.bank.docgen.sharedkernel.document.style;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.Set;

public record MasterStyleCatalogEntry(
        String styleKey,
        Set<String> applicableNodeTypes,
        String renderPurpose
) {

    public MasterStyleCatalogEntry {
        applicableNodeTypes = DefensiveCopies.copySet(applicableNodeTypes);
    }

    public boolean appliesToNodeType(String nodeTypeJson) {
        if (nodeTypeJson == null || nodeTypeJson.isBlank()) {
            return false;
        }
        return applicableNodeTypes.contains(nodeTypeJson.trim());
    }
}
