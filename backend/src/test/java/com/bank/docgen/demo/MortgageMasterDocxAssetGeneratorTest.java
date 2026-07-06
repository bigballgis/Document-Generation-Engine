package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
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

class MortgageMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "mortgage-layout-v2-dual-page";

    private static final Path ASSET = Path.of("..", "deploy", "demo-mortgage", "assets", "mortgage-approval-master.docx");
    static final List<String> ANCHOR_IDS = List.of("MORTGAGE_BODY", "MORTGAGE_SCHEDULE");

    @Test
    void writesMortgageMasterDocxAsset() throws Exception {
        byte[] docx = buildMaster();
        assertThat(new DocxAnchorExtractor().extractOrderedAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyElementsOf(ANCHOR_IDS);
        assertThat(DemoMasterDocxAssertions.readFooterXml(docx)).contains("SECTIONPAGES");
        Files.createDirectories(ASSET.getParent());
        Files.write(ASSET, docx);
    }

    static byte[] buildMaster() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            header.createParagraph().createRun().setText("Meridian Home Finance — Mortgage Services");
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("FCA Regulated Mortgage Lender  |  Meridian Bank plc");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);

            addTitle(document, "Residential Mortgage Approval");
            document.createParagraph().createRun().setText("{{anchor:MORTGAGE_BODY}}");
            XWPFParagraph breakParagraph = document.createParagraph();
            DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(breakParagraph, true);
            addTitle(document, "Repayment Schedule");
            document.createParagraph().createRun().setText("{{anchor:MORTGAGE_SCHEDULE}}");

            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static void addTitle(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText(text);
    }
}
