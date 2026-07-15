package com.bank.docgen.sharedkernel.document.structured;

import java.util.Set;

/**
 * JSON node types the DOCX writer explicitly handles (emit or intentional no-op path).
 * Shared with validation contract tests so the v1 matrix cannot drift into silent-omit gaps
 * (LR-A4 / CD-PIT-07). Includes writer aliases {@code tableComponent} and {@code text}.
 */
public final class DocxWriterHandledStructuredNodeTypes {

    private static final Set<String> JSON_TYPES = Set.of(
            "tableComponentRef",
            "tableComponent",
            "list",
            "imageRef",
            "sealRef",
            "qrBarcodeRef",
            "conditionBlock",
            "loopBlock",
            "contentModuleRef",
            "sectionHeading",
            "paragraph",
            "text",
            "textRun",
            "variable",
            "lineBreak",
            "emphasis",
            "underline",
            "styleRef"
    );

    private DocxWriterHandledStructuredNodeTypes() {
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
