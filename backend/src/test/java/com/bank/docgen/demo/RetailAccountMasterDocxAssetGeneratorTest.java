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
import com.bank.docgen.demo.support.DemoMasterDocxTestAssertions;
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

class RetailAccountMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "retail-account-layout-v3-eight-anchors";
    private static final Path OPEN_ASSET = Path.of("..", "deploy", "demo-retail-account", "assets", "retail-account-open-master.docx");
    private static final Path BALANCE_ASSET = Path.of("..", "deploy", "demo-retail-account", "assets", "retail-account-balance-master.docx");
    static final List<String> OPEN_ANCHOR_IDS = List.of(
            "RAO_PARTIES",
            "RAO_ACCOUNT_OPENING",
            "RAO_PRODUCT_TERMS",
            "RAO_FEE_SCHEDULE",
            "RAO_REGULATORY",
            "RAO_DIGITAL_BANKING",
            "RAO_SIGNATURE",
            "RAO_DISCLAIMER"
    );

    static final List<String> BALANCE_ANCHOR_IDS = List.of(
            "RAB_PARTIES",
            "RAB_BALANCE_CONFIRMATION",
            "RAB_TRANSACTION_SUMMARY",
            "RAB_INTEREST_TAX",
            "RAB_FEE_SCHEDULE",
            "RAB_REGULATORY",
            "RAB_SIGNATURE",
            "RAB_DISCLAIMER"
    );

    /** @deprecated use {@link #OPEN_ANCHOR_IDS} */
    @Deprecated
    static final List<String> OPEN_ANCHORS = OPEN_ANCHOR_IDS;

    /** @deprecated use {@link #BALANCE_ANCHOR_IDS} */
    @Deprecated
    static final List<String> BALANCE_ANCHORS = BALANCE_ANCHOR_IDS;

    @Test
    void writesRetailAccountMasterDocxAssets() throws Exception {
        byte[] openDocx = buildAccountOpeningMaster();
        byte[] balanceDocx = buildBalanceConfirmationMaster();
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(openDocx)))
                .containsExactlyElementsOf(OPEN_ANCHOR_IDS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(balanceDocx)))
                .containsExactlyElementsOf(BALANCE_ANCHOR_IDS);
        assertThat(OPEN_ANCHOR_IDS).hasSize(8);
        assertThat(BALANCE_ANCHOR_IDS).hasSize(8);

        assertBankGradeMaster(openDocx);
        assertBankGradeMaster(balanceDocx);

        String openFooterXml = DemoMasterDocxAssertions.readFooterXml(openDocx);
        assertThat(openFooterXml).contains("NUMPAGES");
        assertThat(openFooterXml).doesNotContain("SECTIONPAGES");
        assertThat(openFooterXml).contains("Customer Service");
        assertThat(openFooterXml).contains("Manchester");
        assertThat(openFooterXml).doesNotContain("Wholesale");

        Files.createDirectories(OPEN_ASSET.getParent());
        com.bank.docgen.demo.support.DemoDeployAssetWriteSupport.writeBestEffort(OPEN_ASSET, openDocx);
        com.bank.docgen.demo.support.DemoDeployAssetWriteSupport.writeBestEffort(BALANCE_ASSET, balanceDocx);
    }

    private static void assertBankGradeMaster(byte[] docx) throws Exception {
        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"SignatureBlock\"")
                .contains("w:styleId=\"TableHeader\"")
                .contains("w:styleId=\"DisclaimerBody\"");

        DemoMasterDocxTestAssertions.assertNoPlaceholderMarkers(docx);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            CTPageMar margins = sectPr.getPgMar();
            long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
            assertThat(((BigInteger) margins.getLeft()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getRight()).longValue()).isGreaterThanOrEqualTo(baseline);
        }
    }


    static byte[] buildAccountOpeningMaster() throws Exception {
        return buildMaster(
                "Account Opening Confirmation",
                "Meridian Retail Banking — Customer Correspondence",
                "Customer Service: 0800 123 4567  |  42 High Street, Manchester M1 1AA  |  FSCS protected deposits",
                OPEN_ANCHOR_IDS,
                List.of(
                        "Parties and Account",
                        "Account Opening Confirmation",
                        "Product Terms and Conditions",
                        "Fee Schedule",
                        "Regulatory Notice",
                        "Digital Banking Services",
                        "Authorised Signatory",
                        "Important Information"
                )
        );
    }

    static byte[] buildBalanceConfirmationMaster() throws Exception {
        return buildMaster(
                "Account Balance Confirmation",
                "Meridian Retail Banking — Customer Correspondence",
                "Customer Service: 0800 123 4567  |  42 High Street, Manchester M1 1AA  |  FSCS protected deposits",
                BALANCE_ANCHOR_IDS,
                List.of(
                        "Parties and Account",
                        "Balance Confirmation",
                        "Transaction Summary",
                        "Interest and Tax",
                        "Applicable Fees",
                        "Regulatory Notice",
                        "Authorised Signatory",
                        "Important Information"
                )
        );
    }

    /**
     * Builds a minimal single-anchor master for binding assembly tests.
     */
    static byte[] buildMaster(String title, String anchorId) throws Exception {
        return buildMaster(
                title,
                "Meridian Retail Banking — Customer Correspondence",
                "Customer Service: 0800 123 4567  |  42 High Street, Manchester M1 1AA",
                List.of(anchorId),
                List.of("Body")
        );
    }

    /**
     * @deprecated use {@link #buildAccountOpeningMaster()} or {@link #buildBalanceConfirmationMaster()}
     */
    @Deprecated
    static byte[] buildMaster(String title, List<String> anchorIds) throws Exception {
        return buildMaster(
                title,
                "Meridian Retail Banking — Customer Correspondence",
                "Customer Service: 0800 123 4567  |  42 High Street, Manchester M1 1AA",
                anchorIds,
                anchorIds.stream().map(id -> "Section").toList()
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
            applyRetailProductStyles(document);

            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFParagraph brandLine = header.createParagraph();
            brandLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun brandRun = brandLine.createRun();
            brandRun.setBold(true);
            brandRun.setFontSize(9);
            brandRun.setColor("006633");
            brandRun.setText(headerBrand);

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

    private static void applyRetailProductStyles(XWPFDocument document) {
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            return;
        }
        DocxMasterStyleRegistry.registerBankParagraphStyle(styles, "DisclaimerBody", 16, "Calibri", false);
    }

    private static void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setColor("006633");
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
