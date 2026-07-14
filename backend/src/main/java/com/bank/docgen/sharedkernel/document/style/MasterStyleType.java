package com.bank.docgen.sharedkernel.document.style;

import java.util.Locale;

/**
 * OOXML style types supported in the persisted master style catalog.
 */
public enum MasterStyleType {
    PARAGRAPH,
    CHARACTER,
    TABLE,
    NUMBERING,
    UNKNOWN;

    public static MasterStyleType fromOoxml(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "paragraph" -> PARAGRAPH;
            case "character" -> CHARACTER;
            case "table" -> TABLE;
            case "numbering" -> NUMBERING;
            default -> UNKNOWN;
        };
    }
}
