package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageNumber;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

/**
 * Build-time helper only — writes the wholesale FOL master DOCX asset under {@code deploy/demo-fol/assets/}.
 * The master is a layout container: page margins, headers, footers, and section-level anchors (ADR-0019).
 * Anchor ids are generated from {@code deploy/demo-fol/generate-fol-catalog.ps1} (hybrid clause/schedule names).
 */
class FolMasterDocxAssetGeneratorTest {

    /** Bump when page layout / header / footer changes; import script uses this to refresh uploaded masters. */
    static final String MASTER_LAYOUT_VERSION = "fol-layout-v5-dual-page-numbering";

    private static final Path ASSET_PATH = Path.of("..", "deploy", "demo-fol", "assets", "wholesale-fol-master.docx");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getHeaderList()).isNotEmpty();
            assertThat(document.getFooterList()).isNotEmpty();
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            assertThat(sectPr.isSetPgMar()).isTrue();
            CTPageMar margins = sectPr.getPgMar();
            assertThat(margins.getLeft()).isNotNull();
            assertThat(sectPr.isSetPgSz()).isTrue();
            CTPageSz pageSize = sectPr.getPgSz();
            assertThat(pageSize.getW()).isEqualTo(BigInteger.valueOf(11906));
            String footerXml = readZipEntryText(docx, "word/footer1.xml");
            assertThat(footerXml).contains("SECTIONPAGES");
            assertThat(footerXml).contains("NUMPAGES");
            assertThat(footerXml).contains("Section Page");
            assertThat(footerXml).contains("Document Page");
        }

        Files.createDirectories(ASSET_PATH.getParent());
        Files.write(ASSET_PATH, docx);
    }

    static byte[] buildWholesaleFolMasterDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurePageLayout(document);
            configureDefaultHeader(document);
            configureDefaultFooter(document);

            addCentered(document, "Meridian Global Banking Corporation", 20, true, "003366");
            addCentered(document, "Wholesale & International Banking", 11, false, "333333");
            addCentered(document, "Facility Offer Letter — Term Loan Facility", 14, true, "000000");
            addCentered(document, "(Confidential — Subject to Contract)", 10, true, "990000");
            document.createParagraph();

            for (String anchorId : ANCHOR_IDS) {
                String title = SECTION_TITLES.getOrDefault(anchorId, anchorId);
                addSection(document, title, anchorId, ANCHOR_IDS.indexOf(anchorId));
            }

            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static List<String> loadAnchorIds() {
        try (InputStream in = FolMasterDocxAssetGeneratorTest.class.getResourceAsStream("/demo/fol-master-anchor-ids.json")) {
            assertThat(in).as("Run deploy/demo-fol/generate-fol-catalog.ps1 first").isNotNull();
            JsonNode root = OBJECT_MAPPER.readTree(in);
            return OBJECT_MAPPER.convertValue(
                    root.get("anchorIds"),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load FOL master anchor ids manifest", ex);
        }
    }

    private static Map<String, String> loadSectionTitles() {
        try (InputStream in = FolMasterDocxAssetGeneratorTest.class.getResourceAsStream("/demo/fol-master-anchor-ids.json")) {
            assertThat(in).as("Run deploy/demo-fol/generate-fol-catalog.ps1 first").isNotNull();
            JsonNode root = OBJECT_MAPPER.readTree(in);
            Map<String, String> titles = new LinkedHashMap<>();
            for (JsonNode section : root.get("sections")) {
                titles.put(section.get("anchorId").asText(), section.get("title").asText());
            }
            return Map.copyOf(titles);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load FOL master section titles manifest", ex);
        }
    }

    private static void configurePageLayout(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();

        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(11906));
        pageSize.setH(BigInteger.valueOf(16838));

        CTPageMar margins = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        margins.setTop(BigInteger.valueOf(1440));
        margins.setBottom(BigInteger.valueOf(1440));
        margins.setLeft(BigInteger.valueOf(1701));
        margins.setRight(BigInteger.valueOf(1276));
        margins.setHeader(BigInteger.valueOf(708));
        margins.setFooter(BigInteger.valueOf(708));
        margins.setGutter(BigInteger.valueOf(0));
    }

    private static void configureDefaultHeader(XWPFDocument document) {
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);

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
        addDualPageNumberRuns(pageLine);

        XWPFParagraph disclaimerLine = footer.createParagraph();
        disclaimerLine.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun disclaimerRun = disclaimerLine.createRun();
        disclaimerRun.setFontSize(7);
        disclaimerRun.setItalic(true);
        disclaimerRun.setColor("888888");
        disclaimerRun.setText("Internal demonstration document — not an offer capable of acceptance");
    }

    private static void addDualPageNumberRuns(XWPFParagraph paragraph) {
        XWPFRun sectionPrefix = paragraph.createRun();
        sectionPrefix.setFontSize(8);
        sectionPrefix.setFontFamily("Calibri");
        sectionPrefix.setText("Section Page ");
        addWordField(paragraph, " PAGE \\* MERGEFORMAT ");
        XWPFRun sectionOf = paragraph.createRun();
        sectionOf.setFontSize(8);
        sectionOf.setFontFamily("Calibri");
        sectionOf.setText(" of ");
        addWordField(paragraph, " SECTIONPAGES \\* MERGEFORMAT ");
        XWPFRun separator = paragraph.createRun();
        separator.setFontSize(8);
        separator.setFontFamily("Calibri");
        separator.setText("  |  Document Page ");
        addWordField(paragraph, " PAGE \\* MERGEFORMAT ");
        XWPFRun documentOf = paragraph.createRun();
        documentOf.setFontSize(8);
        documentOf.setFontFamily("Calibri");
        documentOf.setText(" of ");
        addWordField(paragraph, " NUMPAGES \\* MERGEFORMAT ");
    }

    private static void addWordField(XWPFParagraph paragraph, String instruction) {
        var ctp = paragraph.getCTP();
        CTR begin = ctp.addNewR();
        begin.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        CTR fieldInstruction = ctp.addNewR();
        fieldInstruction.addNewInstrText().setStringValue(instruction);
        CTR separate = ctp.addNewR();
        separate.addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        CTR placeholder = ctp.addNewR();
        placeholder.addNewT().setStringValue("1");
        CTR end = ctp.addNewR();
        end.addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private static void addPageNumberRun(XWPFParagraph paragraph) {
        XWPFRun prefix = paragraph.createRun();
        prefix.setFontSize(8);
        prefix.setFontFamily("Calibri");
        prefix.setText("Page ");
        addWordField(paragraph, " PAGE \\* MERGEFORMAT ");
    }

    private static void addSection(XWPFDocument document, String title, String anchorId, int sectionIndex) {
        if (sectionIndex > 0 && sectionIndex % 8 == 0) {
            appendSectionBreak(document, sectionIndex > 8);
        }
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

    private static void appendSectionBreak(XWPFDocument document, boolean restartPageNumbering) {
        XWPFParagraph breakParagraph = document.createParagraph();
        CTPPr paragraphProperties = breakParagraph.getCTP().isSetPPr()
                ? breakParagraph.getCTP().getPPr()
                : breakParagraph.getCTP().addNewPPr();
        CTSectPr sectionProperties = paragraphProperties.addNewSectPr();
        if (restartPageNumbering) {
            CTPageNumber pageNumber = sectionProperties.addNewPgNumType();
            pageNumber.setStart(BigInteger.ONE);
            pageNumber.setFmt(STNumberFormat.DECIMAL);
        }
    }

    private static String readZipEntryText(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Missing zip entry: " + entryName);
    }
}
