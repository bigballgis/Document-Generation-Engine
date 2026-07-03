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
            registerParagraphStyle(styles, entry.styleKey(), resolveHeadingSize(entry.styleKey()));
        }
    }

    public static String resolveWordStyleId(String styleKey) {
        if (styleKey == null || styleKey.isBlank()) {
            return "Normal";
        }
        return styleKey.trim();
    }

    private static void registerParagraphStyle(XWPFStyles styles, String styleKey, int fontSizeHalfPoints) {
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
        if (fontSizeHalfPoints > 0) {
            ctStyle.addNewRPr().addNewSz().setVal(BigInteger.valueOf(fontSizeHalfPoints));
        }
        styles.addStyle(new XWPFStyle(ctStyle));
    }

    private static int resolveHeadingSize(String styleKey) {
        return switch (styleKey) {
            case "Heading1" -> 32;
            case "Heading2" -> 28;
            case "Heading3" -> 24;
            case "ScheduleTitle" -> 26;
            case "TableHeader" -> 20;
            case "ClauseBody", "BodyText" -> 20;
            default -> 20;
        };
    }
}
