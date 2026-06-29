package com.bank.docgen.authoring.structured;

import java.util.Set;

public record MasterStyleCatalogEntry(
        String styleKey,
        Set<String> applicableNodeTypes,
        String renderPurpose
) {

    public boolean appliesToNodeType(String nodeTypeJson) {
        if (nodeTypeJson == null || nodeTypeJson.isBlank()) {
            return false;
        }
        Set<String> types = applicableNodeTypes == null ? Set.of() : applicableNodeTypes;
        return types.contains(nodeTypeJson.trim());
    }
}
