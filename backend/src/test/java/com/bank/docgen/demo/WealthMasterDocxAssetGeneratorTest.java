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

class WealthMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "wealth-layout-v3-nine-anchors";
    private static final Path ASSET = Path.of("..", "deploy", "demo-wealth", "assets", "wealth-statement-master.docx");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "LOREM|TODO|\\{\\{placeholder|placeholder text",
            Pattern.CASE_INSENSITIVE
    );

    static final List<String> ANCHOR_IDS = List.of(
            "WST_PARTIES",
            "WST_DEFINED_TERMS",
            "WST_PORTFOLIO_SUMMARY",
            "WST_HOLDINGS_EQUITY",
            "WST_HOLDINGS_FIXED_INCOME",
            "WST_HOLDINGS_ALTERNATIVES",
            "WST_INCOME_ACTIVITY",
            "WST_PORTFOLIO_TOTALS",
            "WST_DISCLAIMERS_SIGNATURE"
    );

    /** @deprecated use {@link #ANCHOR_IDS} */
    @Deprecated
    static final List<String> WEALTH_ANCHORS = ANCHOR_IDS;

    @Test
    void writesWealthMasterDocxAsset() throws Exception {
        byte[] docx = buildMaster();
        assertThat(new DocxAnchorExtractor().extractOrderedAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyElementsOf(ANCHOR_IDS);
        assertThat(ANCHOR_IDS).hasSize(9);

        assertBankGradeMaster(docx);

        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("NUMPAGES");
        assertThat(footerXml).doesNotContain("SECTIONPAGES");
        assertThat(footerXml).contains("Past performance is not indicative");

        Files.createDirectories(ASSET.getParent());
        Files.write(ASSET, docx);
    }

    private static void assertBankGradeMaster(byte[] docx) throws Exception {
        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"DefinedTerm\"")
                .contains("w:styleId=\"SignatureBlock\"")
                .contains("w:styleId=\"TableHeader\"")
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

    static byte[] buildMaster() throws Exception {
        return buildMaster(
                "Private Wealth Investment Statement",
                "Meridian Private Wealth — Investment Services",
                "Past performance is not indicative of future results | FCA Client Assets (CASS) | Meridian Private Wealth, 1 St James's Square, London SW1Y 4JU",
                ANCHOR_IDS,
                List.of(
                        "Client and Portfolio",
                        "Defined Terms",
                        "Portfolio Summary",
                        "Equity Holdings",
                        "Fixed Income Holdings",
                        "Alternative Assets",
                        "Income Activity",
                        "Portfolio Totals",
                        "Disclaimers and Signature"
                )
        );
    }

    /**
     * Builds a minimal single-anchor master for binding assembly tests.
     */
    static byte[] buildMaster(String title, String anchorId) throws Exception {
        return buildMaster(
                title,
                "Meridian Private Wealth — Investment Services",
                "Past performance is not indicative of future results | FCA Client Assets (CASS)",
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
            applyWealthProductStyles(document);

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
            confRun.setText("STRICTLY PRIVATE AND CONFIDENTIAL");

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
        DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(document.createParagraph(), true);
    }

    private static void applyWealthProductStyles(XWPFDocument document) {
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            return;
        }
        DocxMasterStyleRegistry.registerBankParagraphStyle(styles, "DisclaimerBody", 16, "Calibri", false);
    }
}
