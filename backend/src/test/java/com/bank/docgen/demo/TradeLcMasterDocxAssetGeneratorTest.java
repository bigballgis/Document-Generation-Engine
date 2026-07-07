package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxLayoutSupport;
import com.bank.docgen.demo.support.DemoMasterDocxPageNumberSupport;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
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
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class TradeLcMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "trade-lc-layout-v3-nine-anchors";
    private static final Path LC_ASSET = Path.of("..", "deploy", "demo-trade-lc", "assets", "trade-lc-notice-master.docx");
    private static final Path GUARANTEE_ASSET = Path.of("..", "deploy", "demo-trade-lc", "assets", "trade-guarantee-notice-master.docx");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "LOREM|TODO|\\{\\{placeholder|placeholder text",
            Pattern.CASE_INSENSITIVE
    );

    static final List<String> LC_ANCHOR_IDS = List.of(
            "TLC_PARTIES",
            "TLC_DEFINED_TERMS",
            "TLC_FACILITY",
            "TLC_SHIPMENT",
            "TLC_DOCUMENTS",
            "TLC_UCP_TERMS",
            "TLC_GOVERNING_LAW",
            "TLC_SIGNATURE",
            "TLC_ATTACHMENT"
    );

    static final List<String> GUARANTEE_ANCHOR_IDS = List.of(
            "TGN_PARTIES",
            "TGN_DEFINED_TERMS",
            "TGN_UNDERTAKING",
            "TGN_CLAIMS",
            "TGN_EXPIRY",
            "TGN_URDG_TERMS",
            "TGN_GOVERNING_LAW",
            "TGN_SIGNATURE",
            "TGN_ATTACHMENT"
    );

    @Test
    void writesTradeLcMasterDocxAssets() throws Exception {
        byte[] lc = buildLcMaster();
        byte[] guarantee = buildGuaranteeMaster();
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(lc))).containsExactlyElementsOf(LC_ANCHOR_IDS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(guarantee)))
                .containsExactlyElementsOf(GUARANTEE_ANCHOR_IDS);
        assertThat(LC_ANCHOR_IDS).hasSize(9);
        assertThat(GUARANTEE_ANCHOR_IDS).hasSize(9);

        assertBankGradeMaster(lc);
        assertBankGradeMaster(guarantee);

        String lcFooterXml = DemoMasterDocxAssertions.readFooterXml(lc);
        assertThat(lcFooterXml).contains("NUMPAGES");
        assertThat(lcFooterXml).doesNotContain("SECTIONPAGES");

        Files.createDirectories(LC_ASSET.getParent());
        Files.write(LC_ASSET, lc);
        Files.write(GUARANTEE_ASSET, guarantee);
    }

    private static void assertBankGradeMaster(byte[] docx) throws Exception {
        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"DefinedTerm\"")
                .contains("w:styleId=\"SignatureBlock\"")
                .contains("w:styleId=\"TableHeader\"");

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

    static byte[] buildLcMaster() throws Exception {
        return buildMaster(
                "Documentary Credit Advice",
                "Meridian Global Banking — Trade Finance Services",
                "UCP 600 / ISBP 745 — Trade Operations Centre, London",
                "Documentary Credit Advice",
                LC_ANCHOR_IDS,
                List.of(
                        "Parties and Recital",
                        "Defined Terms",
                        "Credit Particulars",
                        "Shipment Terms",
                        "Documents Required",
                        "UCP Presentation Terms",
                        "Governing Law",
                        "Authorised Signatory",
                        "Attachments"
                )
        );
    }

    static byte[] buildGuaranteeMaster() throws Exception {
        return buildMaster(
                "Bank Guarantee Notice",
                "Meridian Global Banking — Trade Finance Services",
                "URDG 758 — Demand Guarantees, London Trade Centre",
                "Irrevocable Demand Guarantee",
                GUARANTEE_ANCHOR_IDS,
                List.of(
                        "Parties and Recital",
                        "Defined Terms",
                        "Undertaking Particulars",
                        "Claim Procedure",
                        "Expiry and Reduction",
                        "URDG Standard Terms",
                        "Governing Law",
                        "Authorised Signatory",
                        "Attachments"
                )
        );
    }

    private static byte[] buildMaster(
            String title,
            String headerBrand,
            String footerLegal,
            String coverTitle,
            List<String> anchorIds,
            List<String> sectionTitles
    ) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            DemoMasterDocxStyleSupport.applySharedBankStyles(document);

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
            confRun.setText("WITHOUT ENGAGEMENT — SUBJECT TO UCP / URDG AS APPLICABLE");

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            XWPFParagraph legalLine = footer.createParagraph();
            legalLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun legalRun = legalLine.createRun();
            legalRun.setFontSize(7);
            legalRun.setColor("666666");
            legalRun.setText(footerLegal);
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
        DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(document.createParagraph(), false);
    }
}
