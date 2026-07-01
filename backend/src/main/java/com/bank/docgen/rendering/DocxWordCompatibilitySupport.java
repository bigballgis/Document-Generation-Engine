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
 */
public final class DocxWordCompatibilitySupport {

    private static final BigInteger DOC_DEFAULT_FONT_HALF_POINTS = BigInteger.valueOf(22);
    private static final String DEFAULT_FONT = "Calibri";
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
        if (styles.getNumberOfStyles() == 0) {
            styles.setStyles(buildBaselineCtStyles());
            return;
        }
        styles.setDefaultFonts(buildCalibriFonts());
        styles.setSpellingLanguage(DEFAULT_LANGUAGE);
        applyDocDefaultSizeAndColor(styles);
        if (!styles.styleExist(NORMAL_STYLE_ID)) {
            styles.addStyle(createNormalStyle());
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
        applyDefaultRunProperties(defaultRunProperties);
        docDefaults.addNewPPrDefault().addNewPPr();

        populateNormalStyle(styles.addNewStyle());
        return styles;
    }

    private static XWPFStyle createNormalStyle() {
        CTStyle normal = CTStyle.Factory.newInstance();
        populateNormalStyle(normal);
        return new XWPFStyle(normal);
    }

    private static void populateNormalStyle(CTStyle normal) {
        normal.setType(STStyleType.PARAGRAPH);
        normal.setStyleId(NORMAL_STYLE_ID);
        normal.setDefault(Boolean.TRUE);
        CTString name = CTString.Factory.newInstance();
        name.setVal(NORMAL_STYLE_ID);
        normal.setName(name);
        normal.addNewQFormat();
        applyDefaultRunProperties(normal.addNewRPr());
    }

    private static void applyDocDefaultSizeAndColor(XWPFStyles styles) {
        XWPFDefaultRunStyle defaultRunStyle = styles.getDefaultRunStyle();
        if (defaultRunStyle == null) {
            return;
        }
        applyDefaultRunProperties(defaultRunProperties(defaultRunStyle));
    }

    private static void applyDefaultRunProperties(CTRPr runProperties) {
        CTFonts fonts = runProperties.sizeOfRFontsArray() > 0
                ? runProperties.getRFontsArray(0)
                : runProperties.addNewRFonts();
        applyCalibriFonts(fonts);

        if (runProperties.sizeOfSzArray() == 0) {
            CTHpsMeasure size = runProperties.addNewSz();
            size.setVal(DOC_DEFAULT_FONT_HALF_POINTS);
        }
        if (runProperties.sizeOfSzCsArray() == 0) {
            CTHpsMeasure complexScriptSize = runProperties.addNewSzCs();
            complexScriptSize.setVal(DOC_DEFAULT_FONT_HALF_POINTS);
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

    private static CTFonts buildCalibriFonts() {
        CTFonts fonts = CTFonts.Factory.newInstance();
        applyCalibriFonts(fonts);
        return fonts;
    }

    private static void applyCalibriFonts(CTFonts fonts) {
        fonts.setAscii(DEFAULT_FONT);
        fonts.setHAnsi(DEFAULT_FONT);
        fonts.setCs(DEFAULT_FONT);
        fonts.setEastAsia(DEFAULT_FONT);
    }

    private static CTRPr defaultRunProperties(XWPFDefaultRunStyle defaultRunStyle) {
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
