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

class MortgageMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "mortgage-layout-v3-nine-anchors";
    private static final Path ASSET = Path.of("..", "deploy", "demo-mortgage", "assets", "mortgage-approval-master.docx");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "LOREM|TODO|\\{\\{placeholder|placeholder text",
            Pattern.CASE_INSENSITIVE
    );

    static final List<String> ANCHOR_IDS = List.of(
            "MT_PARTIES",
            "MT_DEFINED_TERMS",
            "MT_LOAN_TERMS",
            "MT_INTEREST",
            "MT_COVENANTS",
            "MT_CONDITIONS",
            "MT_SCHEDULE",
            "MT_GOVERNING_LAW",
            "MT_SIGNATURE"
    );

    @Test
    void writesMortgageMasterDocxAsset() throws Exception {
        byte[] docx = buildMaster();
        assertThat(new DocxAnchorExtractor().extractOrderedAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyElementsOf(ANCHOR_IDS);
        assertThat(ANCHOR_IDS).hasSizeGreaterThanOrEqualTo(9);

        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"DefinedTerm\"")
                .contains("w:styleId=\"SignatureBlock\"")
                .contains("w:styleId=\"TableHeader\"");

        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("SECTIONPAGES");

        assertNoPlaceholderMarkers(docx);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            CTPageMar margins = sectPr.getPgMar();
            long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
            assertThat(((BigInteger) margins.getLeft()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getRight()).longValue()).isGreaterThanOrEqualTo(baseline);
        }

        Files.createDirectories(ASSET.getParent());
        Files.write(ASSET, docx);
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
            brandRun.setText("Meridian Home Finance — Residential Mortgage Services");
            XWPFParagraph regLine = header.createParagraph();
            regLine.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun regRun = regLine.createRun();
            regRun.setBold(true);
            regRun.setFontSize(7);
            regRun.setColor("990000");
            regRun.setText("FCA REGULATED — YOUR HOME MAY BE REPOSSESSED");

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            XWPFParagraph addrLine = footer.createParagraph();
            addrLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun addrRun = addrLine.createRun();
            addrRun.setFontSize(7);
            addrRun.setColor("666666");
            addrRun.setText("PO Box 4400, Manchester M1 4HQ  |  www.meridian-home.example  |  FCA FRN 305207");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);

            addTitle(document, "Residential Mortgage Approval");
            addAnchor(document, "MT_PARTIES");
            insertSectionBreak(document);
            addSectionTitle(document, "Defined Terms");
            addAnchor(document, "MT_DEFINED_TERMS");
            insertSectionBreak(document);
            addSectionTitle(document, "Loan Particulars");
            addAnchor(document, "MT_LOAN_TERMS");
            addAnchor(document, "MT_INTEREST");
            insertSectionBreak(document);
            addSectionTitle(document, "Covenants and Conditions");
            addAnchor(document, "MT_COVENANTS");
            addAnchor(document, "MT_CONDITIONS");
            insertSectionBreak(document);
            addSectionTitle(document, "Repayment Schedule");
            addAnchor(document, "MT_SCHEDULE");
            insertSectionBreak(document);
            addSectionTitle(document, "Closing Provisions");
            addAnchor(document, "MT_GOVERNING_LAW");
            addAnchor(document, "MT_SIGNATURE");

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
