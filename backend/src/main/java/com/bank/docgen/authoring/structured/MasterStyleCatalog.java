package com.bank.docgen.authoring.structured;

import java.util.Map;

public record MasterStyleCatalog(
        String catalogVersion,
        Map<String, MasterStyleCatalogEntry> stylesByKey
) {

    public MasterStyleCatalogEntry find(String styleKey) {
        if (styleKey == null || styleKey.isBlank()) {
            return null;
        }
        Map<String, MasterStyleCatalogEntry> styles = stylesByKey == null ? Map.of() : stylesByKey;
        return styles.get(styleKey.trim());
    }
}
