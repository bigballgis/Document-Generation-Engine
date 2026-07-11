package com.bank.docgen.authorization.management.api;

/**
 * Whitelist sort keys for catalog lists. Unknown values fall back to {@link #GROUP_CODE_ASC}.
 */
public enum CatalogSortKey {
    GROUP_CODE_ASC,
    UPDATED_AT_DESC,
    UPDATED_AT_ASC,
    NAME_ASC,
    EXTERNAL_ID_ASC,
    MODULE_CODE_ASC;

    public static CatalogSortKey parse(String raw, CatalogSortKey... allowedExtras) {
        String key = CatalogPageSupport.blankToNull(raw);
        if (key == null
                || CatalogPageSupport.SORT_GROUP_CODE_ASC.equals(key)
                || CatalogPageSupport.SORT_GROUP_ASC.equals(key)) {
            return GROUP_CODE_ASC;
        }
        CatalogSortKey parsed = switch (key) {
            case CatalogPageSupport.SORT_UPDATED_AT_DESC -> UPDATED_AT_DESC;
            case CatalogPageSupport.SORT_UPDATED_AT_ASC -> UPDATED_AT_ASC;
            case CatalogPageSupport.SORT_NAME_ASC -> NAME_ASC;
            case CatalogPageSupport.SORT_EXTERNAL_ID_ASC -> EXTERNAL_ID_ASC;
            case CatalogPageSupport.SORT_MODULE_CODE_ASC -> MODULE_CODE_ASC;
            default -> null;
        };
        if (parsed == null) {
            return GROUP_CODE_ASC;
        }
        if (parsed == GROUP_CODE_ASC
                || parsed == UPDATED_AT_DESC
                || parsed == UPDATED_AT_ASC
                || parsed == NAME_ASC) {
            return parsed;
        }
        for (CatalogSortKey allowed : allowedExtras) {
            if (parsed == allowed) {
                return parsed;
            }
        }
        return GROUP_CODE_ASC;
    }
}
