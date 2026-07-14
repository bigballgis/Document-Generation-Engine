package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Applies paragraph style refs and run formatting for structured DOCX writing.
 *
 * <p>CE-K02: does not hard-code Calibri/10pt when the master catalog has docDefaults; system
 * baseline is applied only as fail-closed fallback and recorded as {@code MASTER_STYLE_FALLBACK}.
 */
final class StructuredContentDocxStyleSupport {

    private final MasterStyleCatalog styleCatalog;
    private final List<String> fidelityWarningCodes = new ArrayList<>();
    private boolean masterStyleFallbackEmitted;

    StructuredContentDocxStyleSupport(MasterStyleCatalog styleCatalog) {
        this.styleCatalog = styleCatalog;
    }

    List<String> fidelityWarningCodes() {
        return List.copyOf(fidelityWarningCodes);
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
        applyDirectFormatIfPresent(run, null);
        run.setBold(bold);
        run.setItalic(italic);
        if (underline) {
            run.setUnderline(UnderlinePatterns.SINGLE);
        }
        run.setText(text);
    }

    void writeRunText(
            XWPFParagraph paragraph,
            String text,
            boolean bold,
            boolean italic,
            boolean underline,
            JsonNode directFormat
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        XWPFRun run = paragraph.createRun();
        applyDefaultRunStyle(run);
        applyDirectFormatIfPresent(run, directFormat);
        run.setBold(bold);
        run.setItalic(italic);
        if (underline) {
            run.setUnderline(UnderlinePatterns.SINGLE);
        }
        run.setText(text);
    }

    /**
     * CE-K02: omit font/size/color when master docDefaults exist so OOXML inheritance applies.
     * System baseline Calibri/10pt/#000000 only when catalog has no docDefaults (K02-C7).
     */
    void applyDefaultRunStyle(XWPFRun run) {
        if (styleCatalog != null && styleCatalog.hasDocDefaults()) {
            return;
        }
        run.setFontFamily(DocxWordCompatibilitySupport.SYSTEM_FALLBACK_FONT);
        run.setFontSize(10);
        run.setColor("000000");
        emitMasterStyleFallbackOnce();
    }

    static void applyDefaultRunStyle(XWPFRun run, MasterStyleCatalog styleCatalog, Runnable onFallback) {
        if (styleCatalog != null && styleCatalog.hasDocDefaults()) {
            return;
        }
        run.setFontFamily(DocxWordCompatibilitySupport.SYSTEM_FALLBACK_FONT);
        run.setFontSize(10);
        run.setColor("000000");
        if (onFallback != null) {
            onFallback.run();
        }
    }

    private void applyDirectFormatIfPresent(XWPFRun run, JsonNode directFormat) {
        if (directFormat == null || !directFormat.isObject()) {
            return;
        }
        if (directFormat.hasNonNull("fontFamily")) {
            String fontFamily = directFormat.get("fontFamily").asText("").trim();
            if (!fontFamily.isBlank()) {
                run.setFontFamily(fontFamily);
            }
        }
        if (directFormat.hasNonNull("fontSize")) {
            int fontSize = directFormat.get("fontSize").asInt(0);
            if (fontSize > 0) {
                run.setFontSize(fontSize);
            }
        }
        if (directFormat.hasNonNull("textColor")) {
            String color = directFormat.get("textColor").asText("").trim().replace("#", "");
            if (!color.isBlank()) {
                run.setColor(color);
            }
        }
    }

    private void emitMasterStyleFallbackOnce() {
        if (masterStyleFallbackEmitted) {
            return;
        }
        masterStyleFallbackEmitted = true;
        fidelityWarningCodes.add("MASTER_STYLE_FALLBACK");
    }

    record EmphasisStyle(boolean bold, boolean italic) {
    }
}
