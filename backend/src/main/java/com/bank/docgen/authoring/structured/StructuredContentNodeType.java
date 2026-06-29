package com.bank.docgen.authoring.structured;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Confirmed v1 structured-content node matrix (ADR-0019 / P18-T01).
 */
public enum StructuredContentNodeType {

    SECTION_HEADING("sectionHeading", NodeCategory.BLOCK),
    PARAGRAPH("paragraph", NodeCategory.BLOCK),
    LIST("list", NodeCategory.BLOCK),
    CONDITION_BLOCK("conditionBlock", NodeCategory.BLOCK),
    LOOP_BLOCK("loopBlock", NodeCategory.BLOCK),
    TABLE_COMPONENT_REF("tableComponentRef", NodeCategory.BLOCK),

    TEXT_RUN("textRun", NodeCategory.INLINE),
    VARIABLE("variable", NodeCategory.INLINE),
    EMPHASIS("emphasis", NodeCategory.INLINE),
    UNDERLINE("underline", NodeCategory.INLINE),
    LINE_BREAK("lineBreak", NodeCategory.INLINE),

    CONTENT_MODULE_REF("contentModuleRef", NodeCategory.REFERENCE),
    IMAGE_REF("imageRef", NodeCategory.REFERENCE),
    QR_BARCODE_REF("qrBarcodeRef", NodeCategory.REFERENCE),
    SEAL_REF("sealRef", NodeCategory.REFERENCE),
    ATTACHMENT_LIST_REF("attachmentListRef", NodeCategory.REFERENCE),
    STYLE_REF("styleRef", NodeCategory.REFERENCE);

    public enum NodeCategory {
        BLOCK,
        INLINE,
        REFERENCE
    }

    private static final Map<String, StructuredContentNodeType> BY_JSON_TYPE = buildTypeIndex();

    private static Map<String, StructuredContentNodeType> buildTypeIndex() {
        Map<String, StructuredContentNodeType> index = Stream.of(values())
                .collect(Collectors.toMap(StructuredContentNodeType::jsonType, Function.identity()));
        index.put("text", TEXT_RUN);
        return Map.copyOf(index);
    }

    private final String jsonType;
    private final NodeCategory category;

    StructuredContentNodeType(String jsonType, NodeCategory category) {
        this.jsonType = jsonType;
        this.category = category;
    }

    public String jsonType() {
        return jsonType;
    }

    public NodeCategory category() {
        return category;
    }

    public static Optional<StructuredContentNodeType> fromJsonType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawType.trim();
        StructuredContentNodeType direct = BY_JSON_TYPE.get(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }
        return Optional.ofNullable(BY_JSON_TYPE.get(normalized.toLowerCase(Locale.ROOT)));
    }
}
