package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
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
        writeInlineNode(node, paragraph, bold, italic, underline, null);
    }

    void writeInlineNode(
            JsonNode node,
            XWPFParagraph paragraph,
            boolean bold,
            boolean italic,
            boolean underline,
            JsonNode paragraphDirectFormat
    ) {
        String type = node.path("type").asText("");
        // B1-C3 / F9: paragraph whitelist keys on child inline apply to the enclosing
        // XWPFParagraph (after parent apply; same-paragraph conflicts = last-write-wins).
        // Run keys stay on the run via resolveRunDirectFormat + applyDirectFormatIfPresent.
        styles.applyParagraphDirectFormat(paragraph, node.get("directFormat"));
        JsonNode runDirectFormat = StructuredContentDocxStyleSupport.resolveRunDirectFormat(
                node.get("directFormat"),
                paragraphDirectFormat
        );
        if ("text".equals(type) || "textRun".equals(type)) {
            styles.writeRunText(
                    paragraph,
                    node.path("value").asText(""),
                    bold,
                    italic,
                    underline,
                    runDirectFormat
            );
            return;
        }
        if ("variable".equals(type)) {
            String key = node.path("key").asText("");
            Object value = variables.get(key);
            styles.writeRunText(
                    paragraph,
                    value == null ? "" : String.valueOf(value),
                    bold,
                    italic,
                    underline,
                    runDirectFormat
            );
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
            writeInlineChildrenWithStyle(
                    node,
                    paragraph,
                    emphasisStyle.bold(),
                    emphasisStyle.italic(),
                    underline,
                    paragraphDirectFormat
            );
            return;
        }
        if ("underline".equals(type)) {
            writeInlineChildrenWithStyle(node, paragraph, bold, italic, true, paragraphDirectFormat);
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
        if ("qrBarcodeRef".equals(type)) {
            QrBarcodeRefDocxSupport.writeQrBarcodeRef(node, paragraph, variables, styles);
            return;
        }
        rejectIfUnrenderable.accept(type);
    }

    void writeInlineChildren(JsonNode node, XWPFParagraph paragraph) {
        writeInlineChildren(node, paragraph, node.get("directFormat"));
    }

    void writeInlineChildren(JsonNode node, XWPFParagraph paragraph, JsonNode paragraphDirectFormat) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            writeInlineNode(child, paragraph, false, false, false, paragraphDirectFormat);
        }
    }

    void writeInlineChildrenWithStyle(
            JsonNode node,
            XWPFParagraph paragraph,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        writeInlineChildrenWithStyle(node, paragraph, bold, italic, underline, null);
    }

    void writeInlineChildrenWithStyle(
            JsonNode node,
            XWPFParagraph paragraph,
            boolean bold,
            boolean italic,
            boolean underline,
            JsonNode paragraphDirectFormat
    ) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            writeInlineNode(child, paragraph, bold, italic, underline, paragraphDirectFormat);
        }
    }

    private static final int IMAGE_BOX_PT = 48;
    /** Mirrors SealGeometryRules.DEFAULT_SEAL_*_PT (48pt); local to avoid authoring dependency. */
    private static final double DEFAULT_SEAL_PT = 48.0d;

    void writeReferenceNode(JsonNode node, XWPFParagraph paragraph) {
        boolean seal = "sealRef".equals(node.path("type").asText(""));
        StructuredContentImageResolver.ResolvedImage image;
        if (seal) {
            image = imageResolver.resolveSealRef(node.path("referenceKey").asText(""));
        } else {
            image = imageResolver.resolveImageRef(node.path("imageRef").asText(""));
        }
        try {
            XWPFRun run = paragraph.createRun();
            styles.applyDefaultRunStyle(run);
            int widthPt;
            int heightPt;
            if (seal) {
                double declaredWidth = node.path("placement").path("sealBox").path("widthPt").asDouble(DEFAULT_SEAL_PT);
                double declaredHeight = node.path("placement").path("sealBox").path("heightPt").asDouble(DEFAULT_SEAL_PT);
                if (!Double.isFinite(declaredWidth) || declaredWidth <= 0) {
                    declaredWidth = DEFAULT_SEAL_PT;
                }
                if (!Double.isFinite(declaredHeight) || declaredHeight <= 0) {
                    declaredHeight = DEFAULT_SEAL_PT;
                }
                widthPt = (int) Math.round(declaredWidth);
                heightPt = (int) Math.round(declaredHeight);
            } else {
                int[] fitted = fitImageBoxPt(image.bytes());
                widthPt = fitted[0];
                heightPt = fitted[1];
            }
            run.addPicture(
                    new ByteArrayInputStream(image.bytes()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    image.fileName(),
                    Units.toEMU(widthPt),
                    Units.toEMU(heightPt)
            );
        } catch (Exception ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    /** CRCH-W0-2: fit inside 48pt box preserving aspect ratio; unreadable → 48×48. */
    private static int[] fitImageBoxPt(byte[] bytes) {
        try {
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(bytes));
            if (buffered == null || buffered.getWidth() <= 0 || buffered.getHeight() <= 0) {
                return new int[] {IMAGE_BOX_PT, IMAGE_BOX_PT};
            }
            int widthPx = buffered.getWidth();
            int heightPx = buffered.getHeight();
            double scale = Math.min(
                    (double) IMAGE_BOX_PT / widthPx,
                    (double) IMAGE_BOX_PT / heightPx
            );
            int widthPt = Math.max(1, (int) Math.round(widthPx * scale));
            int heightPt = Math.max(1, (int) Math.round(heightPx * scale));
            return new int[] {widthPt, heightPt};
        } catch (IOException | RuntimeException ex) {
            return new int[] {IMAGE_BOX_PT, IMAGE_BOX_PT};
        }
    }
}
