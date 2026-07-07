package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxLayoutSupport;
import com.bank.docgen.demo.support.DemoMasterDocxPageNumberSupport;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.rendering.DocxMasterStyleRegistry;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class CollectionMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "collection-layout-v3-eight-anchors";
    private static final Path RATE_ASSET = Path.of("..", "deploy", "demo-collection", "assets", "rate-change-notice-master.docx");
    private static final Path OVERDUE_ASSET = Path.of("..", "deploy", "demo-collection", "assets", "overdue-collection-master.docx");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "LOREM|TODO|\\{\\{placeholder|placeholder text",
            Pattern.CASE_INSENSITIVE
    );

    static final List<String> RATE_ANCHOR_IDS = List.of(
            "RCN_PARTIES",
            "RCN_RATE_NOTICE",
            "RCN_REGULATORY",
            "RCN_RIGHT_TO_SWITCH",
            "RCN_INTEREST_CALC",
            "RCN_TAX_CONTACT",
            "RCN_SIGNATURE",
            "RCN_DISCLAIMER"
    );

    static final List<String> OVERDUE_ANCHOR_IDS = List.of(
            "OCN_PARTIES",
            "OCN_ARREARS",
            "OCN_REGULATORY",
            "OCN_REPAYMENT",
            "OCN_CONSEQUENCES",
            "OCN_SUPPORT",
            "OCN_SIGNATURE",
            "OCN_DISCLAIMER"
    );

    /** @deprecated use {@link #OVERDUE_ANCHOR_IDS} */
    @Deprecated
    static final List<String> OVERDUE_ANCHORS = OVERDUE_ANCHOR_IDS;

    /** @deprecated use {@link #RATE_ANCHOR_IDS} */
    @Deprecated
    static final List<String> RATE_ANCHORS = RATE_ANCHOR_IDS;

    @Test
    void writesCollectionMasterDocxAssets() throws Exception {
        byte[] rate = buildRateChangeMaster();
        byte[] overdue = buildOverdueMaster();
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(rate))).containsExactlyElementsOf(RATE_ANCHOR_IDS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(overdue)))
                .containsExactlyElementsOf(OVERDUE_ANCHOR_IDS);
        assertThat(RATE_ANCHOR_IDS).hasSize(8);
        assertThat(OVERDUE_ANCHOR_IDS).hasSize(8);

        assertBankGradeMaster(rate);
        assertBankGradeMaster(overdue);

        String rateFooterXml = DemoMasterDocxAssertions.readFooterXml(rate);
        assertThat(rateFooterXml).contains("NUMPAGES");
        assertThat(rateFooterXml).doesNotContain("SECTIONPAGES");
        assertThat(rateFooterXml).contains("FCA CONC");
        assertThat(rateFooterXml).contains("Regulatory collection notice");

        Files.createDirectories(RATE_ASSET.getParent());
        Files.write(RATE_ASSET, rate);
        Files.write(OVERDUE_ASSET, overdue);
    }

    private static void assertBankGradeMaster(byte[] docx) throws Exception {
        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"SignatureBlock\"")
                .contains("w:styleId=\"RegulatoryEmphasis\"")
                .contains("w:styleId=\"DisclaimerBody\"");

        assertNoPlaceholderMarkers(docx);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            CTPageMar margins = sectPr.getPgMar();
            long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
            assertThat(((BigInteger) margins.getLeft()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getRight()).longValue()).isGreaterThanOrEqualTo(baseline);
        }
    }

    private static void assertNoPlaceholderMarkers(byte[] docxBytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n'));
            document.getHeaderList().forEach(header ->
                    header.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n')));
            document.getFooterList().forEach(footer ->
                    footer.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n')));
            String body = text.toString().toUpperCase(Locale.ROOT);
            assertThat(PLACEHOLDER_PATTERN.matcher(body).find())
                    .as("Master DOCX must not contain LOREM/TODO/placeholder markers")
                    .isFalse();
            assertThat(body).doesNotContain("LOREM");
        }
    }

    static byte[] buildRateChangeMaster() throws Exception {
        return buildMaster(
                "Notice of Rate Change",
                "Meridian Retail Banking — Collections & Recoveries",
                "Regulatory collection notice — FCA CONC principles apply | Meridian Retail Banking, PO Box 1200, London EC2N 1DB",
                RATE_ANCHOR_IDS,
                List.of(
                        "Parties and Account",
                        "Rate Variation Notice",
                        "Regulatory Notice",
                        "Right to Switch",
                        "Interest Calculation",
                        "Tax and Contact",
                        "Authorised Signatory",
                        "Important Information"
                )
        );
    }

    static byte[] buildOverdueMaster() throws Exception {
        return buildMaster(
                "Overdue Payment Collection Notice",
                "Meridian Retail Banking — Collections & Recoveries",
                "Regulatory collection notice — FCA CONC principles apply | Meridian Retail Banking, PO Box 1200, London EC2N 1DB",
                OVERDUE_ANCHOR_IDS,
                List.of(
                        "Parties and Account",
                        "Overdue Balance Notice",
                        "Regulatory Notice",
                        "Repayment Options",
                        "Consequences of Non-Payment",
                        "Support and Complaints",
                        "Authorised Signatory",
                        "Important Information"
                )
        );
    }

    /**
     * Builds a minimal single-anchor master for binding assembly tests (BDD-DEMO-EXP-008).
     */
    static byte[] buildMaster(String title, String anchorId) throws Exception {
        return buildMaster(
                title,
                "Meridian Retail Banking — Collections & Recoveries",
                "Regulatory collection notice — FCA CONC principles apply | Meridian Retail Banking, PO Box 1200, London EC2N 1DB",
                List.of(anchorId),
                List.of("Body")
        );
    }

    private static byte[] buildMaster(
            String title,
            String headerBrand,
            String footerDisclaimer,
            List<String> anchorIds,
            List<String> sectionTitles
    ) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            DemoMasterDocxStyleSupport.applySharedBankStyles(document);
            applyCollectionProductStyles(document);

            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFParagraph brandLine = header.createParagraph();
            brandLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun brandRun = brandLine.createRun();
            brandRun.setBold(true);
            brandRun.setFontSize(9);
            brandRun.setColor("003366");
            brandRun.setText(headerBrand);
            XWPFParagraph confLine = header.createParagraph();
            confLine.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun confRun = confLine.createRun();
            confRun.setBold(true);
            confRun.setFontSize(7);
            confRun.setColor("990000");
            confRun.setText("IMPORTANT — PLEASE READ CAREFULLY");

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            XWPFParagraph disclaimerLine = footer.createParagraph();
            disclaimerLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun disclaimerRun = disclaimerLine.createRun();
            disclaimerRun.setFontSize(7);
            disclaimerRun.setColor("666666");
            disclaimerRun.setText(footerDisclaimer);
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addGlobalPageNumberFields(pageLine);

            addTitle(document, title);
            for (int index = 0; index < anchorIds.size(); index++) {
                if (index > 0) {
                    insertSectionBreak(document);
                    addSectionTitle(document, sectionTitles.get(index));
                }
                addAnchor(document, anchorIds.get(index));
            }

            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static void applyCollectionProductStyles(XWPFDocument document) {
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            return;
        }
        DocxMasterStyleRegistry.registerBankParagraphStyle(styles, "RegulatoryEmphasis", 20, "Calibri", true);
        DocxMasterStyleRegistry.registerBankParagraphStyle(styles, "DisclaimerBody", 16, "Calibri", false);
    }

    private static void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText(text);
    }

    private static void addSectionTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(12);
        run.setText(text);
    }

    private static void addAnchor(XWPFDocument document, String anchorId) {
        document.createParagraph().createRun().setText("{{anchor:" + anchorId + "}}");
    }

    private static void insertSectionBreak(XWPFDocument document) {
        DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(document.createParagraph(), false);
    }
}
