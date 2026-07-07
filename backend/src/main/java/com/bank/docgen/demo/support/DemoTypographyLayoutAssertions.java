package com.bank.docgen.demo.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

/**
 * Centralized POI/XML typography and layout assertions for P23 demo master and runtime DOCX (BDD-DEMO-TYP-001…018).
 */
public final class DemoTypographyLayoutAssertions {

  /** Number of discrete checks performed by {@link #assertBankGradeTypography(byte[], PageNumberingProfile)}. */
  public static final int BANK_GRADE_ASSERTION_COUNT = 25;

  private static final Pattern FORBIDDEN_TEXT_PATTERN = Pattern.compile(
      "LOREM|TODO|\\{\\{placeholder|placeholder text",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern STYLE_ASCII_FONT_DIRECT = Pattern.compile("w:ascii=\"([^\"]+)\"");
  private static final Pattern STYLE_ASCII_FONT_VAL = Pattern.compile("w:ascii w:val=\"([^\"]+)\"");

  private DemoTypographyLayoutAssertions() {}

  public enum PageNumberingProfile {
    GLOBAL_ONLY,
    SECTION_AND_GLOBAL
  }

  /**
   * Runs the full P23 bank-grade typography/layout assertion suite on a DOCX byte array.
   *
   * @return {@link #BANK_GRADE_ASSERTION_COUNT} when all checks pass
   */
  public static int assertBankGradeTypography(byte[] docxBytes, PageNumberingProfile profile) throws IOException {
    int count = 0;

    DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docxBytes);
    count += DemoMasterDocxStyleSupport.REQUIRED_BANK_STYLE_KEYS.size();

    String stylesXml = DemoMasterDocxAssertions.readStylesXml(docxBytes);
    assertStylePresent(stylesXml, "TableHeader");
    count++;
    assertStylePresent(stylesXml, "SignatureBlock");
    count++;

    String headingFont = extractStyleAsciiFont(stylesXml, "Heading1");
    String bodyFont = extractStyleAsciiFont(stylesXml, "ClauseBody");
    if (headingFont == null || bodyFont == null) {
      throw new AssertionError("Missing w:rFonts for Heading1 or ClauseBody in styles.xml");
    }
    if (headingFont.equalsIgnoreCase(bodyFont)) {
      throw new AssertionError(
          "Heading1 and ClauseBody must use distinct Latin fonts (TYP-014); both were: " + headingFont);
    }
    count++;
    if (!isApprovedBodyLatinFont(bodyFont)) {
      throw new AssertionError("ClauseBody Latin font must be Calibri or Carlito class; was: " + bodyFont);
    }
    count++;
    if (!isApprovedHeadingLatinFont(headingFont)) {
      throw new AssertionError("Heading1 Latin font must be Cambria or Caladea class; was: " + headingFont);
    }
    count++;

    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      CTSectPr sectPr = document.getDocument().getBody().getSectPr();
      if (sectPr == null || !sectPr.isSetPgMar()) {
        throw new AssertionError("Primary section must define pgMar margins (TYP-018)");
      }
      CTPageMar margins = sectPr.getPgMar();
      long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
      count += assertMarginAtLeast(margins.getLeft(), "left", baseline);
      count += assertMarginAtLeast(margins.getRight(), "right", baseline);
      count += assertMarginAtLeast(margins.getTop(), "top", baseline);
      count += assertMarginAtLeast(margins.getBottom(), "bottom", baseline);

      if (document.getHeaderList().isEmpty()) {
        throw new AssertionError("Master DOCX must include at least one header");
      }
      count++;
      if (document.getFooterList().isEmpty()) {
        throw new AssertionError("Master DOCX must include at least one footer");
      }
      count++;
    }

    count += assertNoForbiddenContentMarkers(docxBytes);

    String footerXml = DemoMasterDocxAssertions.readFooterXml(docxBytes);
    if (!footerXml.contains("NUMPAGES")) {
      throw new AssertionError("Footer must contain document-global NUMPAGES field (TYP-007/008)");
    }
    count++;
    boolean hasSectionPages = footerXml.contains("SECTIONPAGES");
    if (profile == PageNumberingProfile.SECTION_AND_GLOBAL && !hasSectionPages) {
      throw new AssertionError("SECTION_AND_GLOBAL profile requires SECTIONPAGES in footer XML");
    }
    if (profile == PageNumberingProfile.GLOBAL_ONLY && hasSectionPages) {
      throw new AssertionError("GLOBAL_ONLY profile must not contain SECTIONPAGES in footer XML");
    }
    count++;

    if (count != BANK_GRADE_ASSERTION_COUNT) {
      throw new IllegalStateException("Assertion count drift: expected " + BANK_GRADE_ASSERTION_COUNT + " but ran " + count);
    }
    return count;
  }

  public static String readDocumentXml(byte[] docxBytes) throws IOException {
    return DemoMasterDocxAssertions.readDocumentXml(docxBytes);
  }

  public static int assertNoForbiddenContentMarkers(byte[] docxBytes) throws IOException {
    int checks = 0;
    String documentXml = readDocumentXml(docxBytes);
    String upper = documentXml.toUpperCase(Locale.ROOT);
    if (upper.contains("LOREM")) {
      throw new AssertionError("document.xml must not contain LOREM (TYP-013)");
    }
    checks++;
    if (upper.contains("TODO")) {
      throw new AssertionError("document.xml must not contain TODO (TYP-013)");
    }
    checks++;
    if (FORBIDDEN_TEXT_PATTERN.matcher(documentXml).find()) {
      throw new AssertionError("document.xml contains forbidden placeholder markers (TYP-013)");
    }
    checks++;

    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      StringBuilder text = new StringBuilder(documentXml);
      document.getHeaderList().forEach(header ->
          header.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n')));
      document.getFooterList().forEach(footer ->
          footer.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n')));
      document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n'));
      if (FORBIDDEN_TEXT_PATTERN.matcher(text).find()) {
        throw new AssertionError("DOCX extracted text contains forbidden placeholder markers (TYP-013)");
      }
    }
    checks++;
    return checks;
  }

  public static String extractStyleAsciiFont(String stylesXml, String styleId) {
    int styleIdx = stylesXml.indexOf("w:styleId=\"" + styleId + "\"");
    if (styleIdx < 0) {
      return null;
    }
    int styleEnd = stylesXml.indexOf("</w:style>", styleIdx);
    if (styleEnd < 0) {
      styleEnd = stylesXml.length();
    }
    String block = stylesXml.substring(styleIdx, styleEnd);
    Matcher direct = STYLE_ASCII_FONT_DIRECT.matcher(block);
    if (direct.find()) {
      return direct.group(1);
    }
    Matcher valForm = STYLE_ASCII_FONT_VAL.matcher(block);
    return valForm.find() ? valForm.group(1) : null;
  }

  private static void assertStylePresent(String stylesXml, String styleId) {
    if (!stylesXml.contains("w:styleId=\"" + styleId + "\"")) {
      throw new AssertionError("Missing style in styles.xml: " + styleId);
    }
  }

  private static int assertMarginAtLeast(Object marginValue, String side, long baseline) {
    if (marginValue == null) {
      throw new AssertionError("Missing " + side + " margin in pgMar (TYP-018)");
    }
    long twips = ((BigInteger) marginValue).longValue();
    if (twips < baseline) {
      throw new AssertionError(
          side + " margin " + twips + " twips is below bank baseline " + baseline + " twips (TYP-018)");
    }
    return 1;
  }

  private static boolean isApprovedBodyLatinFont(String font) {
    return "Calibri".equalsIgnoreCase(font) || "Carlito".equalsIgnoreCase(font);
  }

  private static boolean isApprovedHeadingLatinFont(String font) {
    return "Cambria".equalsIgnoreCase(font) || "Caladea".equalsIgnoreCase(font);
  }
}
