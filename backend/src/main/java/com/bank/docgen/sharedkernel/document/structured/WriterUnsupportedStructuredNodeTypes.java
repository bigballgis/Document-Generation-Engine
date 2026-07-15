package com.bank.docgen.sharedkernel.document.structured;

import java.util.Set;

/**
 * Single authoritative set of v1 matrix node types that have no DOCX writer branch yet
 * (LR-A4 / CD-PIT-07). Validation, publish gate, and rendering must share this set so
 * templates cannot publish or silently omit these nodes.
 *
 * <p>Lives in sharedkernel so both {@code authoring} and {@code rendering} can depend on it
 * without violating module boundaries.
 *
 * <p>Matching is exact after trim (same camelCase as {@code StructuredContentNodeType#jsonType()}).
 *
 * <p>CE-K06b: {@code qrBarcodeRef} removed after ZXing writer shipped; {@code attachmentListRef}
 * remains until K06c.
 */
public final class WriterUnsupportedStructuredNodeTypes {

    private static final Set<String> JSON_TYPES = Set.of("attachmentListRef");

    private WriterUnsupportedStructuredNodeTypes() {
    }

    public static Set<String> jsonTypes() {
        return JSON_TYPES;
    }

    public static boolean containsJsonType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return false;
        }
        return JSON_TYPES.contains(rawType.trim());
    }
}
