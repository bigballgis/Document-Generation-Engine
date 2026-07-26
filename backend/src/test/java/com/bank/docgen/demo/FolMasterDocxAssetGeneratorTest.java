package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.bank.docgen.demo.support.DemoMasterDocxTestAssertions;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

/**
 * Build-time helper only — writes the wholesale FOL master DOCX asset under {@code deploy/demo-fol/assets/}.
 * The master is a layout container: page margins, headers, footers, and section-level anchors (ADR-0019).
 * Anchor ids are generated from {@code deploy/demo-fol/generate-fol-catalog.ps1} (hybrid clause/schedule names).
 */
class FolMasterDocxAssetGeneratorTest {

    /** Bump when page layout / header / footer / style catalog changes; import script uses this to refresh uploaded masters. */
    static final String MASTER_LAYOUT_VERSION = "fol-layout-v7-logo-letterhead";

    private static final Path ASSET_PATH = Path.of("..", "deploy", "demo-fol", "assets", "wholesale-fol-master.docx");

    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    static final List<String> ANCHOR_IDS = loadAnchorIds();

    static final Map<String, String> SECTION_TITLES = loadSectionTitles();

    @Test
    void writesWholesaleFolMasterDocxAsset() throws Exception {
        byte[] docx = buildWholesaleFolMasterDocx();
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyElementsOf(ANCHOR_IDS);
        assertThat(extractor.extractAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyInAnyOrderElementsOf(ANCHOR_IDS);

        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"DefinedTerm\"")
                .contains("w:styleId=\"SignatureBlock\"")
                .contains("w:styleId=\"TableHeader\"")
                .contains("w:styleId=\"Heading1\"")
                .contains("w:after=\"120\"")
                .contains("w:eastAsia=\"Noto Sans CJK SC\"");

        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("SECTIONPAGES").contains("NUMPAGES");

        DemoMasterDocxTestAssertions.assertNoPlaceholderMarkers(docx);
        // FOS-W15-8: logo letterhead embeds word/media
        assertThat(DemoMasterDocxAssertions.zipEntryNames(docx))
                .anyMatch(name -> name.startsWith("word/media/"));

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getHeaderList()).isNotEmpty();
            assertThat(document.getFooterList()).isNotEmpty();
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            assertThat(sectPr.isSetPgMar()).isTrue();
            CTPageMar margins = sectPr.getPgMar();
            assertThat(margins.getLeft()).isNotNull();
            long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
            assertThat(((BigInteger) margins.getLeft()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getRight()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(sectPr.isSetPgSz()).isTrue();
            CTPageSz pageSize = sectPr.getPgSz();
            assertThat(pageSize.getW()).isEqualTo(BigInteger.valueOf(11906));
        }

        Files.createDirectories(ASSET_PATH.getParent());
        com.bank.docgen.demo.support.DemoDeployAssetWriteSupport.writeBestEffort(ASSET_PATH, docx);
    }

    static byte[] buildWholesaleFolMasterDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            com.bank.docgen.demo.support.DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            DemoMasterDocxStyleSupport.applySharedBankStyles(document);
            configureDefaultHeader(document);
            configureDefaultFooter(document);

            addCentered(document, "Meridian Global Banking Corporation", 20, true, "003366");
            addCentered(document, "Wholesale & International Banking", 11, false, "333333");
            addCentered(document, "Facility Offer Letter — Term Loan Facility", 14, true, "000000");
            addCentered(document, "(Confidential — Subject to Contract)", 10, true, "990000");
            document.createParagraph();

            int sectionIndex = 0;
            for (String anchorId : ANCHOR_IDS) {
                String title = SECTION_TITLES.getOrDefault(anchorId, anchorId);
                if (sectionIndex == 10) {
                    XWPFParagraph breakParagraph = document.createParagraph();
                    com.bank.docgen.demo.support.DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(
                            breakParagraph,
                            true
                    );
                }
                addSection(document, title, anchorId);
                sectionIndex++;
            }

            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }


    private static List<String> loadAnchorIds() {
        try (java.io.InputStream in = FolMasterDocxAssetGeneratorTest.class.getResourceAsStream("/demo/fol-master-anchor-ids.json")) {
            assertThat(in).as("Run deploy/demo-fol/generate-fol-catalog.ps1 first").isNotNull();
            com.fasterxml.jackson.databind.JsonNode root = OBJECT_MAPPER.readTree(in);
            return OBJECT_MAPPER.convertValue(
                    root.get("anchorIds"),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load FOL master anchor ids manifest", ex);
        }
    }

    private static Map<String, String> loadSectionTitles() {
        try (java.io.InputStream in = FolMasterDocxAssetGeneratorTest.class.getResourceAsStream("/demo/fol-master-anchor-ids.json")) {
            assertThat(in).as("Run deploy/demo-fol/generate-fol-catalog.ps1 first").isNotNull();
            com.fasterxml.jackson.databind.JsonNode root = OBJECT_MAPPER.readTree(in);
            Map<String, String> titles = new LinkedHashMap<>();
            for (com.fasterxml.jackson.databind.JsonNode section : root.get("sections")) {
                titles.put(section.get("anchorId").asText(), section.get("title").asText());
            }
            return Map.copyOf(titles);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load FOL master section titles manifest", ex);
        }
    }

    private static void configureDefaultHeader(XWPFDocument document) {
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);

        // FOS-W15-8: real word/media logo (not text-only letterhead).
        XWPFParagraph logoLine = header.createParagraph();
        logoLine.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun logoRun = logoLine.createRun();
        try {
            byte[] logoPng = buildMeridianLogoPng();
            logoRun.addPicture(
                    new ByteArrayInputStream(logoPng),
                    org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG,
                    "meridian-logo.png",
                    org.apache.poi.util.Units.toEMU(36),
                    org.apache.poi.util.Units.toEMU(36)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to embed FOL letterhead logo", ex);
        }

        XWPFParagraph brandLine = header.createParagraph();
        brandLine.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun brandRun = brandLine.createRun();
        brandRun.setBold(true);
        brandRun.setFontSize(9);
        brandRun.setColor("003366");
        brandRun.setFontFamily("Calibri");
        brandRun.setText("Meridian Global Banking Corporation");

        XWPFParagraph confidentialLine = header.createParagraph();
        confidentialLine.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun confidentialRun = confidentialLine.createRun();
        confidentialRun.setBold(true);
        confidentialRun.setFontSize(7);
        confidentialRun.setColor("990000");
        confidentialRun.setFontFamily("Calibri");
        confidentialRun.setText("STRICTLY PRIVATE AND CONFIDENTIAL");

        XWPFParagraph ruleLine = header.createParagraph();
        ruleLine.setBorderBottom(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
        XWPFRun ruleRun = ruleLine.createRun();
        ruleRun.setFontSize(4);
        ruleRun.setText(" ");
    }

    /** Minimal navy square PNG used as KEEP-8 logo letterhead evidence (FOS-W15-8). */
    private static byte[] buildMeridianLogoPng() throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(0x00, 0x33, 0x66));
        graphics.fillRect(0, 0, 64, 64);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(18, 18, 28, 28);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static void configureDefaultFooter(XWPFDocument document) {
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);

        XWPFParagraph addressLine = footer.createParagraph();
        addressLine.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun addressRun = addressLine.createRun();
        addressRun.setFontSize(7);
        addressRun.setColor("666666");
        addressRun.setFontFamily("Calibri");
        addressRun.setText("25 Lombard Street, London EC3V 9AA  |  www.meridian-global.example  |  Regulated by the PRA & FCA");

        XWPFParagraph pageLine = footer.createParagraph();
        pageLine.setAlignment(ParagraphAlignment.CENTER);
        com.bank.docgen.demo.support.DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);

        XWPFParagraph disclaimerLine = footer.createParagraph();
        disclaimerLine.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun disclaimerRun = disclaimerLine.createRun();
        disclaimerRun.setFontSize(7);
        disclaimerRun.setItalic(true);
        disclaimerRun.setColor("888888");
        disclaimerRun.setFontFamily("Calibri");
        disclaimerRun.setText("Internal demonstration document — not an offer capable of acceptance");
    }

    private static void addSection(XWPFDocument document, String title, String anchorId) {
        XWPFParagraph titleParagraph = document.createParagraph();
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(11);
        titleRun.setFontFamily("Calibri");
        titleRun.setText(title);

        XWPFParagraph anchorParagraph = document.createParagraph();
        XWPFRun anchorRun = anchorParagraph.createRun();
        anchorRun.setText("{{anchor:" + anchorId + "}}");
        document.createParagraph();
    }

    private static void addCentered(
            XWPFDocument document,
            String text,
            int fontSize,
            boolean bold,
            String color
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("Calibri");
        if (color != null) {
            run.setColor(color);
        }
        run.setText(text);
    }
}
