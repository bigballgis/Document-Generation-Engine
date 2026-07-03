package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxLayoutSupport;
import com.bank.docgen.demo.support.DemoMasterDocxPageNumberSupport;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

class RetailAccountMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "retail-account-layout-v1-global-page";

    private static final Path OPEN_ASSET = Path.of("..", "deploy", "demo-retail-account", "assets", "retail-account-open-master.docx");
    private static final Path BALANCE_ASSET = Path.of("..", "deploy", "demo-retail-account", "assets", "retail-account-balance-master.docx");

    static final List<String> OPEN_ANCHORS = List.of("RETAIL_OPEN_BODY", "RETAIL_OPEN_TERMS");
    static final List<String> BALANCE_ANCHORS = List.of("RETAIL_BALANCE_BODY", "RETAIL_BALANCE_SUMMARY");

    @Test
    void writesRetailAccountMasterDocxAssets() throws Exception {
        byte[] openDocx = buildMaster("Account Opening Confirmation", OPEN_ANCHORS);
        byte[] balanceDocx = buildMaster("Account Balance Confirmation", BALANCE_ANCHORS);

        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(openDocx))).containsExactlyElementsOf(OPEN_ANCHORS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(balanceDocx))).containsExactlyElementsOf(BALANCE_ANCHORS);

        Files.createDirectories(OPEN_ASSET.getParent());
        Files.write(OPEN_ASSET, openDocx);
        Files.write(BALANCE_ASSET, balanceDocx);
    }

    static byte[] buildMaster(String title, List<String> anchorIds) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            configureHeader(document);
            configureFooter(document);

            addCentered(document, "Meridian Retail Banking", 16, true, "006633");
            addCentered(document, title, 12, true, "000000");
            document.createParagraph();

            for (String anchorId : anchorIds) {
                XWPFParagraph anchorParagraph = document.createParagraph();
                anchorParagraph.createRun().setText("{{anchor:" + anchorId + "}}");
                document.createParagraph();
            }

            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static void configureHeader(XWPFDocument document) {
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
        XWPFParagraph line = header.createParagraph();
        XWPFRun run = line.createRun();
        run.setBold(true);
        run.setFontSize(9);
        run.setColor("006633");
        run.setText("Meridian Retail Banking — Customer Correspondence");
    }

    private static void configureFooter(XWPFDocument document) {
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph address = footer.createParagraph();
        address.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun addressRun = address.createRun();
        addressRun.setFontSize(7);
        addressRun.setColor("666666");
        addressRun.setText("Customer Service: 0800 123 4567  |  42 High Street, Manchester M1 1AA");

        XWPFParagraph pageLine = footer.createParagraph();
        pageLine.setAlignment(ParagraphAlignment.CENTER);
        DemoMasterDocxPageNumberSupport.addGlobalPageNumberFields(pageLine);

        XWPFParagraph disclaimer = footer.createParagraph();
        disclaimer.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun disclaimerRun = disclaimer.createRun();
        disclaimerRun.setFontSize(7);
        disclaimerRun.setItalic(true);
        disclaimerRun.setText("This is a demonstration document — not a contractual offer");
    }

    private static void addCentered(XWPFDocument document, String text, int size, boolean bold, String color) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setFontSize(size);
        run.setColor(color);
        run.setText(text);
    }
}
