package com.bank.docgen.sharedkernel.document.style;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses master DOCX {@code word/styles.xml} (+ optional theme fonts) into a durable style catalog.
 */
public final class MasterDocxStyleCatalogParser {

    private static final String STYLES_PART = "word/styles.xml";
    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String A_NS = "http://schemas.openxmlformats.org/drawingml/2006/main";

    private MasterDocxStyleCatalogParser() {
    }

    public static MasterStyleCatalog parse(byte[] docxBytes) {
        if (docxBytes == null || docxBytes.length == 0) {
            throw new MasterDocxStyleCatalogParseException("DOCX bytes are empty");
        }
        String stylesXml = readZipPart(docxBytes, STYLES_PART);
        if (stylesXml == null || stylesXml.isBlank()) {
            throw new MasterDocxStyleCatalogParseException("word/styles.xml is missing");
        }
        String themeXml = readFirstThemePart(docxBytes);
        try {
            Document stylesDoc = parseXml(stylesXml);
            MasterStyleDocDefaults docDefaults = parseDocDefaults(stylesDoc);
            Map<String, MasterStyleCatalogEntry> styles = parseStyles(stylesDoc);
            MasterStyleThemeFonts themeFonts = themeXml == null ? null : parseThemeFonts(themeXml);
            String catalogVersion = "master-styles-" + sha256Prefix(stylesXml);
            return new MasterStyleCatalog(catalogVersion, styles, docDefaults, themeFonts);
        } catch (MasterDocxStyleCatalogParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MasterDocxStyleCatalogParseException("Unable to parse word/styles.xml", ex);
        }
    }

    public static MasterStyleCatalog parse(InputStream docxStream) {
        try {
            return parse(docxStream.readAllBytes());
        } catch (IOException ex) {
            throw new MasterDocxStyleCatalogParseException("Unable to read DOCX stream", ex);
        }
    }

    private static Map<String, MasterStyleCatalogEntry> parseStyles(Document stylesDoc) {
        Map<String, MasterStyleCatalogEntry> styles = new LinkedHashMap<>();
        NodeList styleNodes = stylesDoc.getElementsByTagNameNS(W_NS, "style");
        if (styleNodes.getLength() == 0) {
            styleNodes = stylesDoc.getElementsByTagName("w:style");
        }
        for (int index = 0; index < styleNodes.getLength(); index++) {
            Node node = styleNodes.item(index);
            if (!(node instanceof Element styleElement)) {
                continue;
            }
            String styleId = attr(styleElement, "styleId");
            if (styleId == null || styleId.isBlank()) {
                continue;
            }
            MasterStyleType styleType = MasterStyleType.fromOoxml(attr(styleElement, "type"));
            MasterStyleTypography typography = parseRunTypography(firstChild(styleElement, "rPr"));
            Set<String> applicable = baselineApplicableNodeTypes(styleType);
            String renderPurpose = baselineRenderPurpose(styleType, styleId);
            styles.put(
                    styleId,
                    new MasterStyleCatalogEntry(styleId, applicable, renderPurpose, styleType, typography)
            );
        }
        return styles;
    }

    private static MasterStyleDocDefaults parseDocDefaults(Document stylesDoc) {
        Element docDefaults = firstElement(stylesDoc, "docDefaults");
        if (docDefaults == null) {
            return null;
        }
        Element rPrDefault = firstChild(docDefaults, "rPrDefault");
        if (rPrDefault == null) {
            return null;
        }
        Element rPr = firstChild(rPrDefault, "rPr");
        if (rPr == null) {
            return null;
        }
        MasterStyleTypography typography = parseRunTypography(rPr);
        if (typography == null) {
            return null;
        }
        MasterStyleDocDefaults defaults = new MasterStyleDocDefaults(
                typography.ascii(),
                typography.hAnsi(),
                typography.eastAsia(),
                typography.cs(),
                typography.fontSizeHalfPoints(),
                typography.color()
        );
        if (!defaults.hasAnyFontSlot() && !defaults.hasFontSize()) {
            return null;
        }
        return defaults;
    }

    private static MasterStyleTypography parseRunTypography(Element rPr) {
        if (rPr == null) {
            return null;
        }
        Element fonts = firstChild(rPr, "rFonts");
        String ascii = fonts == null ? null : attr(fonts, "ascii");
        String hAnsi = fonts == null ? null : attr(fonts, "hAnsi");
        String eastAsia = fonts == null ? null : attr(fonts, "eastAsia");
        String cs = fonts == null ? null : attr(fonts, "cs");
        Integer size = parseHalfPoints(firstChild(rPr, "sz"));
        Boolean bold = firstChild(rPr, "b") == null ? null : Boolean.TRUE;
        Boolean italic = firstChild(rPr, "i") == null ? null : Boolean.TRUE;
        Element colorElement = firstChild(rPr, "color");
        String color = colorElement == null ? null : attr(colorElement, "val");
        MasterStyleTypography typography = new MasterStyleTypography(
                blankToNull(ascii),
                blankToNull(hAnsi),
                blankToNull(eastAsia),
                blankToNull(cs),
                size,
                bold,
                italic,
                blankToNull(color)
        );
        if (!typography.hasAnyFontSlot()
                && !typography.hasFontSize()
                && typography.bold() == null
                && typography.italic() == null
                && typography.color() == null) {
            return null;
        }
        return typography;
    }

    private static MasterStyleThemeFonts parseThemeFonts(String themeXml) throws Exception {
        Document themeDoc = parseXml(themeXml);
        Element major = firstElement(themeDoc, "majorFont");
        Element minor = firstElement(themeDoc, "minorFont");
        if (major == null && minor == null) {
            // drawingml may use namespaced local names via getElementsByTagNameNS
            NodeList majorList = themeDoc.getElementsByTagNameNS(A_NS, "majorFont");
            NodeList minorList = themeDoc.getElementsByTagNameNS(A_NS, "minorFont");
            if (majorList.getLength() > 0) {
                major = (Element) majorList.item(0);
            }
            if (minorList.getLength() > 0) {
                minor = (Element) minorList.item(0);
            }
        }
        if (major == null && minor == null) {
            return null;
        }
        return new MasterStyleThemeFonts(
                typeface(major, "latin"),
                typeface(major, "ea"),
                typeface(major, "cs"),
                typeface(minor, "latin"),
                typeface(minor, "ea"),
                typeface(minor, "cs")
        );
    }

    private static String typeface(Element fontGroup, String localName) {
        if (fontGroup == null) {
            return null;
        }
        Element child = firstChild(fontGroup, localName);
        if (child == null) {
            return null;
        }
        return blankToNull(attr(child, "typeface"));
    }

    static Set<String> baselineApplicableNodeTypes(MasterStyleType styleType) {
        return switch (styleType) {
            case PARAGRAPH -> Set.of("paragraph", "sectionHeading", "list");
            case CHARACTER -> Set.of("textRun", "emphasis", "underline");
            default -> Set.of();
        };
    }

    static String baselineRenderPurpose(MasterStyleType styleType, String styleId) {
        String key = styleId == null ? "" : styleId.toLowerCase(Locale.ROOT);
        if (key.contains("heading") || key.contains("title")) {
            return "HEADING";
        }
        if (key.contains("table")) {
            return "TABLE";
        }
        return styleType == MasterStyleType.CHARACTER ? "INLINE" : "BODY";
    }

    private static Integer parseHalfPoints(Element sz) {
        if (sz == null) {
            return null;
        }
        String value = attr(sz, "val");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Element firstElement(Document document, String localName) {
        NodeList byNs = document.getElementsByTagNameNS("*", localName);
        if (byNs.getLength() > 0 && byNs.item(0) instanceof Element element) {
            return element;
        }
        NodeList byPrefixed = document.getElementsByTagName("w:" + localName);
        if (byPrefixed.getLength() > 0 && byPrefixed.item(0) instanceof Element element) {
            return element;
        }
        NodeList byA = document.getElementsByTagName("a:" + localName);
        if (byA.getLength() > 0 && byA.item(0) instanceof Element element) {
            return element;
        }
        return null;
    }

    private static Element firstChild(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element
                    && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        // Fallback for parsers that do not expose localName
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element) {
                String name = element.getNodeName();
                if (localName.equals(name)
                        || name.endsWith(":" + localName)) {
                    return element;
                }
            }
        }
        return null;
    }

    private static String attr(Element element, String localName) {
        if (element.hasAttributeNS(W_NS, localName)) {
            return element.getAttributeNS(W_NS, localName);
        }
        if (element.hasAttributeNS(A_NS, localName)) {
            return element.getAttributeNS(A_NS, localName);
        }
        if (element.hasAttribute(localName)) {
            return element.getAttribute(localName);
        }
        if (element.hasAttribute("w:" + localName)) {
            return element.getAttribute("w:" + localName);
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String readZipPart(byte[] docxBytes, String partName) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (partName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
                entry = zip.getNextEntry();
            }
        } catch (IOException ex) {
            throw new MasterDocxStyleCatalogParseException("Unable to read DOCX zip part " + partName, ex);
        }
        return null;
    }

    private static String readFirstThemePart(byte[] docxBytes) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (name.startsWith("word/theme/") && name.endsWith(".xml")) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
                entry = zip.getNextEntry();
            }
        } catch (IOException ex) {
            return null;
        }
        return null;
    }

    private static String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
