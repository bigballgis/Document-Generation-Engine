package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Applies paragraph style refs and run formatting for structured DOCX writing.
 *
 * <p>CE-K02: does not hard-code Calibri/10pt when the master catalog has docDefaults; system
 * baseline is applied only as fail-closed fallback and recorded as {@code MASTER_STYLE_FALLBACK}.
 *
 * <p>IBL-B1: whitelisted paragraph spacing/indents are applied on {@link XWPFParagraph}; run
 * whitelist keys remain on {@link XWPFRun}.
 */
final class StructuredContentDocxStyleSupport {

    private static final double TWIPS_PER_POINT = 20.0;

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

    /**
     * Applies whitelisted paragraph spacing/indent keys from {@code directFormat} (pt → twips;
     * {@code lineSpacing} as AUTO multiple). Absent keys are left unset so style/master inherit.
     */
    void applyParagraphDirectFormat(XWPFParagraph paragraph, JsonNode directFormat) {
        if (paragraph == null || directFormat == null || !directFormat.isObject()) {
            return;
        }
        if (directFormat.hasNonNull("spacingBefore")) {
            paragraph.setSpacingBefore(pointsToTwips(directFormat.get("spacingBefore").asDouble()));
        }
        if (directFormat.hasNonNull("spacingAfter")) {
            paragraph.setSpacingAfter(pointsToTwips(directFormat.get("spacingAfter").asDouble()));
        }
        if (directFormat.hasNonNull("lineSpacing")) {
            paragraph.setSpacingBetween(directFormat.get("lineSpacing").asDouble(), LineSpacingRule.AUTO);
        }
        if (directFormat.hasNonNull("leftIndent")) {
            paragraph.setIndentationLeft(pointsToTwips(directFormat.get("leftIndent").asDouble()));
        }
        if (directFormat.hasNonNull("rightIndent")) {
            paragraph.setIndentationRight(pointsToTwips(directFormat.get("rightIndent").asDouble()));
        }
        if (directFormat.hasNonNull("firstLineIndent")) {
            paragraph.setIndentationFirstLine(pointsToTwips(directFormat.get("firstLineIndent").asDouble()));
        }
    }

    /**
     * Prefer child run {@code directFormat}; fall back to paragraph-level font keys only.
     */
    static JsonNode resolveRunDirectFormat(JsonNode nodeDirectFormat, JsonNode paragraphDirectFormat) {
        if (nodeDirectFormat != null && nodeDirectFormat.isObject()) {
            if (paragraphDirectFormat == null || !paragraphDirectFormat.isObject()) {
                return nodeDirectFormat;
            }
            ObjectNode merged = ((ObjectNode) paragraphDirectFormat).deepCopy();
            nodeDirectFormat.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));
            return merged;
        }
        return paragraphDirectFormat;
    }

    void writeRunText(
            XWPFParagraph paragraph,
            String text,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        writeRunText(paragraph, text, bold, italic, underline, null);
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

    private static int pointsToTwips(double points) {
        return (int) Math.round(points * TWIPS_PER_POINT);
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
