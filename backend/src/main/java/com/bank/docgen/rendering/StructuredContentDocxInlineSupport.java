package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

final class StructuredContentDocxInlineSupport {

    private final Map<String, Object> variables;
    private final StructuredContentDocxStyleSupport styles;
    private final StructuredContentImageResolver imageResolver;
    private final Consumer<String> rejectIfUnrenderable;

    StructuredContentDocxInlineSupport(
            Map<String, Object> variables,
            StructuredContentDocxStyleSupport styles,
            StructuredContentImageResolver imageResolver,
            Consumer<String> rejectIfUnrenderable
    ) {
        this.variables = variables;
        this.styles = styles;
        this.imageResolver = imageResolver;
        this.rejectIfUnrenderable = rejectIfUnrenderable;
    }

    void writeInlineNode(
            JsonNode node,
            XWPFParagraph paragraph,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        String type = node.path("type").asText("");
        if ("text".equals(type) || "textRun".equals(type)) {
            styles.writeRunText(paragraph, node.path("value").asText(""), bold, italic, underline);
            return;
        }
        if ("variable".equals(type)) {
            String key = node.path("key").asText("");
            Object value = variables.get(key);
            styles.writeRunText(paragraph, value == null ? "" : String.valueOf(value), bold, italic, underline);
            return;
        }
        if ("lineBreak".equals(type)) {
            XWPFRun run = paragraph.createRun();
            styles.applyDefaultRunStyle(run);
            run.addBreak();
            return;
        }
        if ("emphasis".equals(type)) {
            StructuredContentDocxStyleSupport.EmphasisStyle emphasisStyle = styles.resolveEmphasis(node);
            writeInlineChildrenWithStyle(node, paragraph, emphasisStyle.bold(), emphasisStyle.italic(), underline);
            return;
        }
        if ("underline".equals(type)) {
            writeInlineChildrenWithStyle(node, paragraph, bold, italic, true);
            return;
        }
        if ("styleRef".equals(type)) {
            String styleKey = node.path("styleRef").asText("");
            if (styles.styleExists(styleKey)) {
                styles.applyParagraphStyle(paragraph, styleKey);
            }
            return;
        }
        if ("imageRef".equals(type) || "sealRef".equals(type)) {
            writeReferenceNode(node, paragraph);
            return;
        }
        rejectIfUnrenderable.accept(type);
    }

    void writeInlineChildren(JsonNode node, XWPFParagraph paragraph) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            writeInlineNode(child, paragraph, false, false, false);
        }
    }

    void writeInlineChildrenWithStyle(
            JsonNode node,
            XWPFParagraph paragraph,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            writeInlineNode(child, paragraph, bold, italic, underline);
        }
    }

    void writeReferenceNode(JsonNode node, XWPFParagraph paragraph) {
        StructuredContentImageResolver.ResolvedImage image;
        if ("sealRef".equals(node.path("type").asText(""))) {
            image = imageResolver.resolveSealRef(node.path("referenceKey").asText(""));
        } else {
            image = imageResolver.resolveImageRef(node.path("imageRef").asText(""));
        }
        try {
            XWPFRun run = paragraph.createRun();
            styles.applyDefaultRunStyle(run);
            run.addPicture(
                    new java.io.ByteArrayInputStream(image.bytes()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    image.fileName(),
                    Units.toEMU(48),
                    Units.toEMU(48)
            );
        } catch (Exception ex) {
            throw new DocxAssemblyException(ex);
        }
    }
}
