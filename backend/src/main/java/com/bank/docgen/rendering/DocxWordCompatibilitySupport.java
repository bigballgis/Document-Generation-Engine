package com.bank.docgen.rendering;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;
import org.apache.poi.xwpf.usermodel.XWPFDefaultRunStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFSettings;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocDefaults;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLanguage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

/**
 * Ensures POI-generated DOCX packages include the minimum Word-compatible styles and settings
 * parts so Microsoft Word can render body text instead of showing a blank page.
 *
 * <p>CE-K02: when the master package already has styles / docDefaults, those fonts are preserved
 * — Calibri baseline is only applied for empty/missing styles parts (system fallback path).
 */
public final class DocxWordCompatibilitySupport {

    /** System baseline half-points (10pt) — only for missing docDefaults (K02-C7). */
    public static final BigInteger SYSTEM_FALLBACK_FONT_HALF_POINTS = BigInteger.valueOf(20);
    public static final String SYSTEM_FALLBACK_FONT = "Calibri";
    private static final String DEFAULT_TEXT_COLOR = "000000";
    private static final String DEFAULT_LANGUAGE = "en-US";
    private static final String NORMAL_STYLE_ID = "Normal";

    private DocxWordCompatibilitySupport() {
    }

    public static void ensureWordCompatiblePackage(XWPFDocument document) {
        ensureStyles(document);
        ensureSettings(document);
        ensureSectionPropertiesLast(document);
    }

    private static void ensureStyles(XWPFDocument document) {
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            styles = document.createStyles();
            styles.setStyles(buildBaselineCtStyles());
            return;
        }
        if (styles.getNumberOfStyles() == 0 && !hasDocDefaults(styles)) {
            styles.setStyles(buildBaselineCtStyles());
            return;
        }
        // CE-K02: preserve master docDefaults / default fonts — do not overwrite with Calibri.
        styles.setSpellingLanguage(DEFAULT_LANGUAGE);
        if (!styles.styleExist(NORMAL_STYLE_ID)) {
            styles.addStyle(createNormalStyleWithoutHardcodedFonts());
        }
    }

    private static boolean hasDocDefaults(XWPFStyles styles) {
        try {
            XWPFDefaultRunStyle defaultRunStyle = styles.getDefaultRunStyle();
            return defaultRunStyle != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void ensureSettings(XWPFDocument document) {
        XWPFSettings settings = document.getSettings();
        if (settings == null) {
            return;
        }
        settings.setZoomPercent(100L);
    }

    private static void ensureSectionPropertiesLast(XWPFDocument document) {
        CTBody body = document.getDocument().getBody();
        if (!body.isSetSectPr()) {
            return;
        }
        String sectionXml = null;
        try {
            sectionXml = body.getSectPr().xmlText();
            CTSectPr moved = CTSectPr.Factory.parse(sectionXml);
            body.unsetSectPr();
            body.setSectPr(moved);
        } catch (Exception ex) {
            if (!body.isSetSectPr() && sectionXml != null) {
                try {
                    body.setSectPr(CTSectPr.Factory.parse(sectionXml));
                } catch (Exception ignored) {
                    // Best-effort Word compatibility; leave body without sectPr rather than fail assembly.
                }
            }
        }
    }

    private static CTStyles buildBaselineCtStyles() {
        CTStyles styles = CTStyles.Factory.newInstance();

        CTDocDefaults docDefaults = styles.addNewDocDefaults();
        CTRPr defaultRunProperties = docDefaults.addNewRPrDefault().addNewRPr();
        applySystemFallbackRunProperties(defaultRunProperties);
        docDefaults.addNewPPrDefault().addNewPPr();

        populateNormalStyleWithFallback(styles.addNewStyle());
        return styles;
    }

    private static XWPFStyle createNormalStyleWithoutHardcodedFonts() {
        CTStyle normal = CTStyle.Factory.newInstance();
        normal.setType(STStyleType.PARAGRAPH);
        normal.setStyleId(NORMAL_STYLE_ID);
        normal.setDefault(Boolean.TRUE);
        CTString name = CTString.Factory.newInstance();
        name.setVal(NORMAL_STYLE_ID);
        normal.setName(name);
        normal.addNewQFormat();
        return new XWPFStyle(normal);
    }

    private static void populateNormalStyleWithFallback(CTStyle normal) {
        normal.setType(STStyleType.PARAGRAPH);
        normal.setStyleId(NORMAL_STYLE_ID);
        normal.setDefault(Boolean.TRUE);
        CTString name = CTString.Factory.newInstance();
        name.setVal(NORMAL_STYLE_ID);
        normal.setName(name);
        normal.addNewQFormat();
        applySystemFallbackRunProperties(normal.addNewRPr());
    }

    private static void applySystemFallbackRunProperties(CTRPr runProperties) {
        CTFonts fonts = runProperties.sizeOfRFontsArray() > 0
                ? runProperties.getRFontsArray(0)
                : runProperties.addNewRFonts();
        fonts.setAscii(SYSTEM_FALLBACK_FONT);
        fonts.setHAnsi(SYSTEM_FALLBACK_FONT);
        fonts.setCs(SYSTEM_FALLBACK_FONT);
        fonts.setEastAsia(SYSTEM_FALLBACK_FONT);

        if (runProperties.sizeOfSzArray() == 0) {
            CTHpsMeasure size = runProperties.addNewSz();
            size.setVal(SYSTEM_FALLBACK_FONT_HALF_POINTS);
        }
        if (runProperties.sizeOfSzCsArray() == 0) {
            CTHpsMeasure complexScriptSize = runProperties.addNewSzCs();
            complexScriptSize.setVal(SYSTEM_FALLBACK_FONT_HALF_POINTS);
        }
        if (runProperties.sizeOfColorArray() == 0) {
            CTColor color = runProperties.addNewColor();
            color.setVal(DEFAULT_TEXT_COLOR);
        }
        if (runProperties.sizeOfLangArray() == 0) {
            CTLanguage language = runProperties.addNewLang();
            language.setVal(DEFAULT_LANGUAGE);
        }
    }

    /**
     * Exposed for tests that need to read private default run properties via the same path.
     */
    static CTRPr defaultRunProperties(XWPFDefaultRunStyle defaultRunStyle) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    XWPFDefaultRunStyle.class,
                    MethodHandles.lookup());
            MethodHandle getter = lookup.findVirtual(
                    XWPFDefaultRunStyle.class,
                    "getRPr",
                    MethodType.methodType(CTRPr.class));
            return (CTRPr) getter.invoke(defaultRunStyle);
        } catch (Throwable ex) {
            throw new DocxAssemblyException(ex);
        }
    }
}
