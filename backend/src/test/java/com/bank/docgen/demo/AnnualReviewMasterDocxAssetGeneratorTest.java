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

class AnnualReviewMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "annual-review-layout-v3-nine-anchors";
    private static final Path REVIEW_ASSET = Path.of("..", "deploy", "demo-annual-review", "assets", "annual-review-master.docx");
    private static final Path RENEWAL_ASSET = Path.of("..", "deploy", "demo-annual-review", "assets", "facility-renewal-master.docx");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "LOREM|TODO|\\{\\{placeholder|placeholder text",
            Pattern.CASE_INSENSITIVE
    );

    static final List<String> REVIEW_ANCHOR_IDS = List.of(
            "ARR_PARTIES",
            "ARR_DEFINED_TERMS",
            "ARR_REVIEW_SUMMARY",
            "ARR_PRICING",
            "ARR_COVENANTS",
            "ARR_FINDINGS",
            "ARR_CONTINUATION",
            "ARR_GOVERNING_LAW",
            "ARR_SIGNATURE"
    );

    static final List<String> RENEWAL_ANCHOR_IDS = List.of(
            "FRN_PARTIES",
            "FRN_RENEWAL_CONFIRM",
            "FRN_RENEWED_TERMS",
            "FRN_COVENANTS",
            "FRN_CONDITIONS",
            "FRN_KYC",
            "FRN_SCHEDULE",
            "FRN_GOVERNING_LAW",
            "FRN_SIGNATURE"
    );

    /** @deprecated use {@link #REVIEW_ANCHOR_IDS} */
    @Deprecated
    static final List<String> REVIEW_ANCHORS = REVIEW_ANCHOR_IDS;

    /** @deprecated use {@link #RENEWAL_ANCHOR_IDS} */
    @Deprecated
    static final List<String> RENEWAL_ANCHORS = RENEWAL_ANCHOR_IDS;

    @Test
    void writesAnnualReviewMasterDocxAssets() throws Exception {
        byte[] review = buildAnnualReviewMaster();
        byte[] renewal = buildFacilityRenewalMaster();
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(review)))
                .containsExactlyElementsOf(REVIEW_ANCHOR_IDS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(renewal)))
                .containsExactlyElementsOf(RENEWAL_ANCHOR_IDS);
        assertThat(REVIEW_ANCHOR_IDS).hasSize(9);
        assertThat(RENEWAL_ANCHOR_IDS).hasSize(9);

        assertBankGradeMaster(review);
        assertBankGradeMaster(renewal);

        String reviewFooterXml = DemoMasterDocxAssertions.readFooterXml(review);
        assertThat(reviewFooterXml).contains("SECTIONPAGES");
        assertThat(reviewFooterXml).contains("NUMPAGES");
        assertThat(reviewFooterXml).contains("Regulated by the PRA");

        Files.createDirectories(REVIEW_ASSET.getParent());
        Files.write(REVIEW_ASSET, review);
        Files.write(RENEWAL_ASSET, renewal);
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

    static byte[] buildAnnualReviewMaster() throws Exception {
        return buildMaster(
                "Annual Credit Review",
                "Meridian Global Banking Corporation — Corporate Credit",
                REVIEW_ANCHOR_IDS,
                List.of(
                        "Parties and Recital",
                        "Defined Terms",
                        "Review Summary",
                        "Pricing Adjustment",
                        "Covenant Compliance",
                        "Material Findings",
                        "Continued Availability",
                        "Governing Law",
                        "Authorised Signatory"
                )
        );
    }

    static byte[] buildFacilityRenewalMaster() throws Exception {
        return buildMaster(
                "Facility Renewal Confirmation",
                "Meridian Global Banking Corporation — Corporate Credit",
                RENEWAL_ANCHOR_IDS,
                List.of(
                        "Parties and Recital",
                        "Renewal Confirmation",
                        "Renewed Terms",
                        "Financial Covenants",
                        "Conditions Precedent",
                        "KYC and AML Requirements",
                        "Covenant Compliance Schedule",
                        "Governing Law",
                        "Authorised Signatory"
                )
        );
    }

    /**
     * Builds a minimal single-anchor master for binding assembly tests.
     */
    static byte[] buildMaster(String title, String anchorId) throws Exception {
        return buildMaster(
                title,
                "Meridian Global Banking Corporation — Corporate Credit",
                List.of(anchorId),
                List.of("Body")
        );
    }

    private static byte[] buildMaster(
            String title,
            String headerBrand,
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
            confRun.setText("STRICTLY PRIVATE AND CONFIDENTIAL");

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            XWPFParagraph addrLine = footer.createParagraph();
            addrLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun addrRun = addrLine.createRun();
            addrRun.setFontSize(7);
            addrRun.setColor("666666");
            addrRun.setText("25 Lombard Street, London EC3V 9AA  |  www.meridian-global.example  |  Regulated by the PRA & FCA");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);

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
}
