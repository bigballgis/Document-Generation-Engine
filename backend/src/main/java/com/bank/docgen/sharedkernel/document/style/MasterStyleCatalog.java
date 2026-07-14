package com.bank.docgen.sharedkernel.document.style;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.Map;

public record MasterStyleCatalog(
        String catalogVersion,
        Map<String, MasterStyleCatalogEntry> stylesByKey,
        MasterStyleDocDefaults docDefaults,
        MasterStyleThemeFonts themeFonts
) {

    public MasterStyleCatalog {
        stylesByKey = DefensiveCopies.copyMap(stylesByKey);
    }

    /**
     * Backward-compatible constructor for platform metadata catalogs without typography.
     */
    public MasterStyleCatalog(String catalogVersion, Map<String, MasterStyleCatalogEntry> stylesByKey) {
        this(catalogVersion, stylesByKey, null, null);
    }

    public MasterStyleCatalogEntry find(String styleKey) {
        if (styleKey == null || styleKey.isBlank()) {
            return null;
        }
        return stylesByKey.get(styleKey.trim());
    }

    public boolean hasDocDefaults() {
        return docDefaults != null
                && (docDefaults.hasAnyFontSlot() || docDefaults.hasFontSize());
    }
}
