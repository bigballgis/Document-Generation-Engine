package com.bank.docgen.rendering;

import com.bank.docgen.authoring.structured.MasterStyleCatalog;
import com.bank.docgen.authoring.structured.MasterStyleCatalogEntry;
import java.math.BigInteger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

/**
 * Registers paragraph styles from the approved master style catalog into a DOCX package.
 */
public final class DocxMasterStyleRegistry {

    private DocxMasterStyleRegistry() {
    }

    public static void ensureCatalogStyles(XWPFDocument document, MasterStyleCatalog catalog) {
        if (catalog == null || catalog.stylesByKey() == null || catalog.stylesByKey().isEmpty()) {
            return;
        }
        DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            return;
        }
        for (MasterStyleCatalogEntry entry : catalog.stylesByKey().values()) {
            registerBankParagraphStyle(
                    styles,
                    entry.styleKey(),
                    resolveDefaultFontSizeHalfPoints(entry.styleKey()),
                    "Calibri",
                    isBoldStyle(entry.styleKey())
            );
        }
    }

    public static void registerBankParagraphStyle(
            XWPFStyles styles,
            String styleKey,
            int fontSizeHalfPoints,
            String fontFamily,
            boolean bold
    ) {
        registerParagraphStyle(styles, styleKey, fontSizeHalfPoints, fontFamily, bold);
    }

    public static int resolveDefaultFontSizeHalfPoints(String styleKey) {
        return resolveHeadingSize(styleKey);
    }

    public static String resolveWordStyleId(String styleKey) {
        if (styleKey == null || styleKey.isBlank()) {
            return "Normal";
        }
        return styleKey.trim();
    }

    private static void registerParagraphStyle(
            XWPFStyles styles,
            String styleKey,
            int fontSizeHalfPoints,
            String fontFamily,
            boolean bold
    ) {
        String styleId = resolveWordStyleId(styleKey);
        if (styles.styleExist(styleId)) {
            return;
        }
        CTStyle ctStyle = CTStyle.Factory.newInstance();
        ctStyle.setType(STStyleType.PARAGRAPH);
        ctStyle.setStyleId(styleId);
        CTString name = CTString.Factory.newInstance();
        name.setVal(styleId);
        ctStyle.setName(name);
        ctStyle.addNewQFormat();
        var runProperties = ctStyle.addNewRPr();
        if (fontSizeHalfPoints > 0) {
            runProperties.addNewSz().setVal(BigInteger.valueOf(fontSizeHalfPoints));
        }
        if (fontFamily != null && !fontFamily.isBlank()) {
            var fonts = runProperties.addNewRFonts();
            fonts.setAscii(fontFamily);
            fonts.setHAnsi(fontFamily);
            fonts.setCs(fontFamily);
        }
        if (bold) {
            runProperties.addNewB();
        }
        styles.addStyle(new XWPFStyle(ctStyle));
    }

    private static boolean isBoldStyle(String styleKey) {
        return switch (styleKey) {
            case "Heading1", "Heading2", "Heading3", "ScheduleTitle", "TableHeader", "DefinedTerm" -> true;
            default -> false;
        };
    }

    private static int resolveHeadingSize(String styleKey) {
        return switch (styleKey) {
            case "Heading1" -> 32;
            case "Heading2" -> 28;
            case "Heading3" -> 24;
            case "ScheduleTitle" -> 26;
            case "TableHeader" -> 20;
            case "DefinedTerm" -> 20;
            case "SignatureBlock" -> 20;
            case "ClauseBody", "BodyText" -> 20;
            default -> 20;
        };
    }
}
