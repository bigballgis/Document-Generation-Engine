package com.bank.docgen.sharedkernel.document.style;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.Set;

public record MasterStyleCatalogEntry(
        String styleKey,
        Set<String> applicableNodeTypes,
        String renderPurpose,
        MasterStyleType styleType,
        MasterStyleTypography typography
) {

    public MasterStyleCatalogEntry {
        applicableNodeTypes = DefensiveCopies.copySet(applicableNodeTypes);
        if (styleType == null) {
            styleType = MasterStyleType.UNKNOWN;
        }
    }

    /**
     * Backward-compatible constructor for platform metadata entries without typography.
     */
    public MasterStyleCatalogEntry(String styleKey, Set<String> applicableNodeTypes, String renderPurpose) {
        this(styleKey, applicableNodeTypes, renderPurpose, MasterStyleType.UNKNOWN, null);
    }

    public boolean appliesToNodeType(String nodeTypeJson) {
        if (nodeTypeJson == null || nodeTypeJson.isBlank()) {
            return false;
        }
        return applicableNodeTypes.contains(nodeTypeJson.trim());
    }
}
