package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

final class StructuredContentDocxStyleSupport {

    private final MasterStyleCatalog styleCatalog;

    StructuredContentDocxStyleSupport(MasterStyleCatalog styleCatalog) {
        this.styleCatalog = styleCatalog;
    }

    String resolveStyleRef(JsonNode node, String fallback) {
        if (node.has("styleRef") && !node.get("styleRef").isNull()) {
            String styleRef = node.get("styleRef").asText("").trim();
            if (!styleRef.isBlank() && styleCatalog.find(styleRef) != null) {
                return styleRef;
            }
        }
        return fallback;
    }

    void applyParagraphStyle(XWPFParagraph paragraph, String styleKey) {
        paragraph.setStyle(DocxMasterStyleRegistry.resolveWordStyleId(styleKey));
    }

    boolean styleExists(String styleKey) {
        return styleCatalog.find(styleKey) != null;
    }

    EmphasisStyle resolveEmphasis(JsonNode node) {
        String variant = node.path("variant").asText("bold").trim().toLowerCase(Locale.ROOT);
        return switch (variant) {
            case "italic" -> new EmphasisStyle(false, true);
            case "bolditalic", "bold_italic" -> new EmphasisStyle(true, true);
            default -> new EmphasisStyle(true, false);
        };
    }

    void writeRunText(
            XWPFParagraph paragraph,
            String text,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        XWPFRun run = paragraph.createRun();
        applyDefaultRunStyle(run);
        run.setBold(bold);
        run.setItalic(italic);
        if (underline) {
            run.setUnderline(UnderlinePatterns.SINGLE);
        }
        run.setText(text);
    }

    static void applyDefaultRunStyle(XWPFRun run) {
        run.setFontFamily("Calibri");
        run.setFontSize(10);
        run.setColor("000000");
    }

    record EmphasisStyle(boolean bold, boolean italic) {
    }
}
