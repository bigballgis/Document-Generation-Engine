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

class AnnualReviewMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "annual-review-layout-v1-dual-page";
    private static final Path REVIEW_ASSET = Path.of("..", "deploy", "demo-annual-review", "assets", "annual-review-master.docx");
    private static final Path RENEWAL_ASSET = Path.of("..", "deploy", "demo-annual-review", "assets", "facility-renewal-master.docx");
    static final List<String> REVIEW_ANCHORS = List.of("ANNUAL_REVIEW_BODY", "ANNUAL_REVIEW_SCHEDULE");
    static final List<String> RENEWAL_ANCHORS = List.of("FACILITY_RENEWAL_BODY", "FACILITY_RENEWAL_TERMS");

    @Test
    void writesAnnualReviewMasterDocxAssets() throws Exception {
        byte[] review = buildMaster("Annual Credit Review", REVIEW_ANCHORS);
        byte[] renewal = buildMaster("Facility Renewal Letter", RENEWAL_ANCHORS);
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(review))).containsExactlyElementsOf(REVIEW_ANCHORS);
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(renewal))).containsExactlyElementsOf(RENEWAL_ANCHORS);
        Files.createDirectories(REVIEW_ASSET.getParent());
        Files.write(REVIEW_ASSET, review);
        Files.write(RENEWAL_ASSET, renewal);
    }

    static byte[] buildMaster(String title, List<String> anchors) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            document.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("Meridian Corporate Banking — Relationship Management");
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("Confidential — For authorised recipients only");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);
            addTitle(document, title);
            document.createParagraph().createRun().setText("{{anchor:" + anchors.get(0) + "}}");
            DemoMasterDocxLayoutSupport.insertSectionBreakNextPage(document.createParagraph(), true);
            addTitle(document, "Schedule");
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
