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

class TradeLcMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "trade-lc-layout-v2-global-page";
    private static final Path LC_ASSET = Path.of("..", "deploy", "demo-trade-lc", "assets", "trade-lc-notice-master.docx");
    private static final Path GUARANTEE_ASSET = Path.of("..", "deploy", "demo-trade-lc", "assets", "trade-guarantee-notice-master.docx");
    static final List<String> LC_ANCHORS = List.of("TRADE_LC_BODY", "TRADE_LC_ATTACHMENT");
    static final List<String> GUARANTEE_ANCHORS = List.of("TRADE_GUARANTEE_BODY", "TRADE_GUARANTEE_ATTACHMENT");

    @Test
    void writesTradeLcMasterDocxAssets() throws Exception {
        byte[] lc = buildMaster("Documentary Credit Advice", LC_ANCHORS);
        byte[] guarantee = buildMaster("Bank Guarantee Notice", GUARANTEE_ANCHORS);
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(lc))).containsExactlyElementsOf(LC_ANCHORS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(guarantee))).containsExactlyElementsOf(GUARANTEE_ANCHORS);
        Files.createDirectories(LC_ASSET.getParent());
        Files.write(LC_ASSET, lc);
        Files.write(GUARANTEE_ASSET, guarantee);
    }

    static byte[] buildMaster(String title, List<String> anchors) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            document.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("Meridian Trade Finance — Documentary Credits");
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("UCP 600 / ISBP — Trade Operations Centre, London");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addGlobalPageNumberFields(pageLine);
            addTitle(document, title);
            document.createParagraph().createRun().setText("{{anchor:" + anchors.get(0) + "}}");
            DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(document.createParagraph(), false);
            addTitle(document, "Attachments");
            document.createParagraph().createRun().setText("{{anchor:" + anchors.get(1) + "}}");
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
