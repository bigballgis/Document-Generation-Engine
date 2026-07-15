package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.expression.ConditionExpressionEvaluator;
import com.bank.docgen.sharedkernel.document.structured.DocxWriterHandledStructuredNodeTypes;
import com.bank.docgen.sharedkernel.document.structured.WriterUnsupportedStructuredNodeTypes;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * Package-private block-node type dispatch for StructuredContentDocxWriteSession.
 * OOXML write semantics stay in the session methods this delegates to.
 */
final class StructuredContentDocxBlockDispatchSupport {

    private static final ConditionExpressionEvaluator CONDITION_EVALUATOR = ConditionExpressionEvaluator.INSTANCE;

    private final StructuredContentDocxStyleSupport styles;
    private final StructuredContentDocxInlineSupport inlineSupport;
    private final java.util.Map<String, Object> variables;
    private final java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeLoopBlock;
    private final java.util.function.BiConsumer<JsonNode, XWPFParagraph> expandContentModule;
    private final java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeSectionHeading;
    private final java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeInlineOrBlockChildren;
    private final java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeAttachmentListRef;

    StructuredContentDocxBlockDispatchSupport(
            StructuredContentDocxStyleSupport styles,
            StructuredContentDocxInlineSupport inlineSupport,
            java.util.Map<String, Object> variables,
            java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeLoopBlock,
            java.util.function.BiConsumer<JsonNode, XWPFParagraph> expandContentModule,
            java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeSectionHeading,
            java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeInlineOrBlockChildren,
            java.util.function.BiConsumer<JsonNode, XWPFParagraph> writeAttachmentListRef
    ) {
        this.styles = styles;
        this.inlineSupport = inlineSupport;
        this.variables = variables;
        this.writeLoopBlock = writeLoopBlock;
        this.expandContentModule = expandContentModule;
        this.writeSectionHeading = writeSectionHeading;
        this.writeInlineOrBlockChildren = writeInlineOrBlockChildren;
        this.writeAttachmentListRef = writeAttachmentListRef;
    }

    void rejectIfUnrenderable(String type) {
        if (WriterUnsupportedStructuredNodeTypes.containsJsonType(type)
                || !DocxWriterHandledStructuredNodeTypes.containsJsonType(type)) {
            throw new DocxAssemblyException(
                    "api.error.rendering.unsupportedNodeType",
                    "Unsupported structured content node type: " + type
            );
        }
    }

    void writeBlockNode(JsonNode node, XWPFParagraph paragraph) {
        String type = node.path("type").asText("");
        rejectIfUnrenderable(type);
        if ("conditionBlock".equals(type)) {
            if (CONDITION_EVALUATOR.evaluate(node.path("conditionExpression").asText(""), variables)) {
                writeInlineOrBlockChildren.accept(node, paragraph);
            }
            return;
        }
        if ("loopBlock".equals(type)) {
            writeLoopBlock.accept(node, paragraph);
            return;
        }
        if ("contentModuleRef".equals(type)) {
            expandContentModule.accept(node, paragraph);
            return;
        }
        if ("attachmentListRef".equals(type)) {
            writeAttachmentListRef.accept(node, paragraph);
            return;
        }
        if ("sectionHeading".equals(type)) {
            styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "Heading1"));
            writeSectionHeading.accept(node, paragraph);
            return;
        }
        if ("paragraph".equals(type)) {
            JsonNode legacyText = node.get("text");
            if (legacyText != null && legacyText.isTextual()) {
                styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "BodyText"));
                styles.writeRunText(paragraph, legacyText.asText(""), false, false, false);
                return;
            }
            styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "BodyText"));
            inlineSupport.writeInlineChildren(node, paragraph);
            return;
        }
        if ("text".equals(type) || "textRun".equals(type)) {
            styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "BodyText"));
            styles.writeRunText(paragraph, node.path("value").asText(""), false, false, false);
            return;
        }
        inlineSupport.writeInlineChildren(node, paragraph);
    }
}
