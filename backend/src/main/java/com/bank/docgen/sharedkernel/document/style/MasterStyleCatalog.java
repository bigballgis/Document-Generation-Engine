package com.bank.docgen.sharedkernel.document.style;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.Map;

public record MasterStyleCatalog(
        String catalogVersion,
        Map<String, MasterStyleCatalogEntry> stylesByKey
) {

    public MasterStyleCatalog {
        stylesByKey = DefensiveCopies.copyMap(stylesByKey);
    }

    public MasterStyleCatalogEntry find(String styleKey) {
        if (styleKey == null || styleKey.isBlank()) {
            return null;
        }
        return stylesByKey.get(styleKey.trim());
    }
}
