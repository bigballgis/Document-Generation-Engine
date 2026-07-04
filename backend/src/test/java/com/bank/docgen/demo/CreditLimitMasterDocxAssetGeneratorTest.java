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
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

class CreditLimitMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "credit-limit-layout-v1-dual-page";
    private static final Path ASSET = Path.of("..", "deploy", "demo-credit-limit", "assets", "credit-limit-master.docx");
    static final List<String> ANCHOR_IDS = List.of("CREDIT_LIMIT_BODY", "CREDIT_LIMIT_TERMS");

    @Test
    void writesCreditLimitMasterDocxAsset() throws Exception {
        byte[] docx = buildMaster();
        assertThat(new DocxAnchorExtractor().extractOrderedAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyElementsOf(ANCHOR_IDS);
        Files.createDirectories(ASSET.getParent());
        Files.write(ASSET, docx);
    }

    static byte[] buildMaster() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            document.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("Meridian Corporate Banking — Credit Facilities");
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("Strictly Private and Confidential — Corporate Banking");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);
            addTitle(document, "Credit Limit Confirmation");
            document.createParagraph().createRun().setText("{{anchor:CREDIT_LIMIT_BODY}}");
            DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(document.createParagraph(), true);
            addTitle(document, "Facility Terms");
            document.createParagraph().createRun().setText("{{anchor:CREDIT_LIMIT_TERMS}}");
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
