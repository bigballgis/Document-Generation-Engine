package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.bank.docgen.sharedkernel.document.style.MasterStyleTypography;
import java.math.BigInteger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

/**
 * Registers paragraph styles from the approved master style catalog into a DOCX package.
 *
 * <p>CE-K02: fonts/sizes come from per-master catalog typography — never hard-coded Calibri
 * heuristics when catalog typography is present.
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
            MasterStyleTypography typography = entry.typography();
            String fontFamily = resolveFontFamily(typography, catalog);
            int fontSizeHalfPoints = resolveFontSizeHalfPoints(typography, catalog);
            boolean bold = typography != null && Boolean.TRUE.equals(typography.bold());
            registerBankParagraphStyle(
                    styles,
                    entry.styleKey(),
                    fontSizeHalfPoints,
                    fontFamily,
                    bold,
                    typography == null ? null : typography.eastAsia(),
                    0,
                    0
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
        registerBankParagraphStyle(styles, styleKey, fontSizeHalfPoints, fontFamily, bold, null, 0, 0);
    }

    /**
     * FOS-W15-5: optional eastAsia font + paragraph spacing twips from bank style manifest.
     */
    public static void registerBankParagraphStyle(
            XWPFStyles styles,
            String styleKey,
            int fontSizeHalfPoints,
            String fontFamily,
            boolean bold,
            String eastAsiaFontFamily,
            int spacingBeforeTwips,
            int spacingAfterTwips
    ) {
        registerParagraphStyle(
                styles,
                styleKey,
                fontSizeHalfPoints,
                fontFamily,
                bold,
                eastAsiaFontFamily,
                spacingBeforeTwips,
                spacingAfterTwips
        );
    }

    /**
     * @deprecated CE-K02: style-key heuristics removed from production path; kept for demo asset generators.
     */
    @Deprecated
    public static int resolveDefaultFontSizeHalfPoints(String styleKey) {
        return 20;
    }

    public static String resolveWordStyleId(String styleKey) {
        if (styleKey == null || styleKey.isBlank()) {
            return "Normal";
        }
        return styleKey.trim();
    }

    private static String resolveFontFamily(MasterStyleTypography typography, MasterStyleCatalog catalog) {
        if (typography != null) {
            if (typography.eastAsia() != null && !typography.eastAsia().isBlank()) {
                return typography.eastAsia();
            }
            if (typography.ascii() != null && !typography.ascii().isBlank()) {
                return typography.ascii();
            }
            if (typography.hAnsi() != null && !typography.hAnsi().isBlank()) {
                return typography.hAnsi();
            }
        }
        if (catalog != null && catalog.hasDocDefaults()) {
            if (catalog.docDefaults().eastAsia() != null) {
                return catalog.docDefaults().eastAsia();
            }
            if (catalog.docDefaults().ascii() != null) {
                return catalog.docDefaults().ascii();
            }
        }
        // Only when catalog lacks typography + docDefaults (K02-C7 path for registration).
        return null;
    }

    private static int resolveFontSizeHalfPoints(MasterStyleTypography typography, MasterStyleCatalog catalog) {
        if (typography != null && typography.hasFontSize()) {
            return typography.fontSizeHalfPoints();
        }
        if (catalog != null && catalog.hasDocDefaults() && catalog.docDefaults().hasFontSize()) {
            return catalog.docDefaults().fontSizeHalfPoints();
        }
        return 0;
    }

    private static void registerParagraphStyle(
            XWPFStyles styles,
            String styleKey,
            int fontSizeHalfPoints,
            String fontFamily,
            boolean bold,
            String eastAsiaFontFamily,
            int spacingBeforeTwips,
            int spacingAfterTwips
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
        boolean hasLatin = fontFamily != null && !fontFamily.isBlank();
        boolean hasEastAsia = eastAsiaFontFamily != null && !eastAsiaFontFamily.isBlank();
        if (hasLatin || hasEastAsia) {
            var fonts = runProperties.addNewRFonts();
            String latin = hasLatin ? fontFamily : eastAsiaFontFamily;
            fonts.setAscii(latin);
            fonts.setHAnsi(latin);
            fonts.setCs(latin);
            fonts.setEastAsia(hasEastAsia ? eastAsiaFontFamily : latin);
        }
        if (bold) {
            runProperties.addNewB();
        }
        if (spacingBeforeTwips > 0 || spacingAfterTwips > 0) {
            var paragraphProperties = ctStyle.addNewPPr();
            var spacing = paragraphProperties.addNewSpacing();
            if (spacingBeforeTwips > 0) {
                spacing.setBefore(BigInteger.valueOf(spacingBeforeTwips));
            }
            if (spacingAfterTwips > 0) {
                spacing.setAfter(BigInteger.valueOf(spacingAfterTwips));
            }
        }
        styles.addStyle(new XWPFStyle(ctStyle));
    }
}
