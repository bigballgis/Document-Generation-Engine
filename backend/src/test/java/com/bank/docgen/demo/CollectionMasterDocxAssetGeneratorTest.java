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

class CollectionMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "collection-layout-v2-global-page";
    private static final Path RATE_ASSET = Path.of("..", "deploy", "demo-collection", "assets", "rate-change-notice-master.docx");
    private static final Path OVERDUE_ASSET = Path.of("..", "deploy", "demo-collection", "assets", "overdue-collection-master.docx");
    static final List<String> RATE_ANCHORS = List.of("RATE_CHANGE_BODY");
    static final List<String> OVERDUE_ANCHORS = List.of("OVERDUE_COLLECTION_BODY");

    @Test
    void writesCollectionMasterDocxAssets() throws Exception {
        byte[] rate = buildMaster("Notice of Rate Change", RATE_ANCHORS.get(0));
        byte[] overdue = buildMaster("Overdue Payment Collection Notice", OVERDUE_ANCHORS.get(0));
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(rate))).containsExactlyElementsOf(RATE_ANCHORS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(overdue))).containsExactlyElementsOf(OVERDUE_ANCHORS);
        Files.createDirectories(RATE_ASSET.getParent());
        Files.write(RATE_ASSET, rate);
        Files.write(OVERDUE_ASSET, overdue);
    }

    static byte[] buildMaster(String title, String anchorId) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            document.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("Meridian Retail Banking — Collections & Recoveries");
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("Regulatory collection notice — FCA CONC principles apply");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addGlobalPageNumberFields(pageLine);
            addTitle(document, title);
            document.createParagraph().createRun().setText("{{anchor:" + anchorId + "}}");
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
