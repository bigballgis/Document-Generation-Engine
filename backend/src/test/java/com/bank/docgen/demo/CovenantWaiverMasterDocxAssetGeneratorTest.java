package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxLayoutSupport;
import com.bank.docgen.demo.support.DemoMasterDocxPageNumberSupport;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
import com.bank.docgen.demo.support.DemoMasterDocxTestAssertions;
import com.bank.docgen.demo.support.DemoPackageContractSupport;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class CovenantWaiverMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "covenant-waiver-layout-v1-eight-anchors";
    private static final Path ASSET = Path.of("..", "deploy", "demo-covenant-waiver", "assets", "covenant-waiver-master.docx");
    private static final String PACKAGE_CODE = "demo-covenant-waiver";

    static final List<String> ANCHOR_IDS = List.of(
            "CW_PARTIES",
            "CW_DEFINED_TERMS",
            "CW_WAIVER",
            "CW_SCOPE",
            "CW_CONDITIONS",
            "CW_RESERVATION",
            "CW_GOVERNING_LAW",
            "CW_SIGNATURE"
    );

    @Test
    void writesMasterDocxAsset() throws Exception {
        byte[] docx = buildMaster();
        assertThat(new DocxAnchorExtractor().extractOrderedAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyElementsOf(ANCHOR_IDS);
        assertThat(ANCHOR_IDS).hasSizeGreaterThanOrEqualTo(7);

        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"DefinedTerm\"")
                .contains("w:styleId=\"SignatureBlock\"")
                .contains("w:styleId=\"TableHeader\"");

        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("SECTIONPAGES");

        DemoMasterDocxTestAssertions.assertNoPlaceholderMarkers(docx);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            CTPageMar margins = sectPr.getPgMar();
            long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
            assertThat(((BigInteger) margins.getLeft()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getRight()).longValue()).isGreaterThanOrEqualTo(baseline);
        }

        Files.createDirectories(ASSET.getParent());
        com.bank.docgen.demo.support.DemoDeployAssetWriteSupport.writeBestEffort(ASSET, docx);
    }

    @Test
    void packageSourcesMeetBankLetterContentQuality() throws Exception {
        Path root = DemoPackageContractSupport.packageRoot(PACKAGE_CODE);
        String bindings = Files.readString(root.resolve("config").resolve(
                PACKAGE_CODE.replace("demo-", "") + "-binding-overlays.json"), StandardCharsets.UTF_8);
        String sql = readSql(root.resolve("sql"));
        String fixtures = Files.readString(root.resolve("config").resolve(
                PACKAGE_CODE.replace("demo-", "") + "-demo-test-variables.json"), StandardCharsets.UTF_8);
        String combined = bindings + "\n" + sql + "\n" + fixtures;
        String lower = combined.toLowerCase(Locale.ROOT);

        assertThat(lower).doesNotContain("lorem");
        assertThat(lower).doesNotContain("placeholder");
        assertThat(lower).doesNotContain("for the executive demonstration dataset");
        assertThat(lower).doesNotContain("will be expanded in the final documentation set");
        assertThat(combined).contains("SignatureBlock");
        assertThat(combined).containsIgnoringCase("governing");

        for (String marker : List.of(
                "Northgate Manufacturing",
                "Specified Covenant Breach",
                "Waiver Period",
                "does not waive",
                "Meridian Global Banking Corporation"
        )) {
            assertThat(combined).contains(marker);
        }
    }

    static byte[] buildMaster() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            DemoMasterDocxStyleSupport.applySharedBankStyles(document);

            org.apache.poi.xwpf.usermodel.XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            org.apache.poi.xwpf.usermodel.XWPFParagraph brandLine = header.createParagraph();
            brandLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun brandRun = brandLine.createRun();
            brandRun.setBold(true);
            brandRun.setFontSize(9);
            brandRun.setColor("003366");
            brandRun.setText("Meridian Global Banking Corporation — Corporate Credit");
            org.apache.poi.xwpf.usermodel.XWPFParagraph confLine = header.createParagraph();
            confLine.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun confRun = confLine.createRun();
            confRun.setBold(true);
            confRun.setFontSize(7);
            confRun.setColor("990000");
            confRun.setText("STRICTLY PRIVATE AND CONFIDENTIAL");

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            org.apache.poi.xwpf.usermodel.XWPFParagraph addrLine = footer.createParagraph();
            addrLine.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun addrRun = addrLine.createRun();
            addrRun.setFontSize(7);
            addrRun.setColor("666666");
            addrRun.setText("25 Lombard Street, London EC3V 9AA  |  www.meridian-global.example  |  Regulated by the PRA & FCA");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);

            addTitle(document, "Covenant Waiver / Consent Letter");
            addSectionTitle(document, "Parties");
            addAnchor(document, "CW_PARTIES");
            insertSectionBreak(document);
            addSectionTitle(document, "Defined Terms");
            addAnchor(document, "CW_DEFINED_TERMS");
            insertSectionBreak(document);
            addSectionTitle(document, "Waiver Scope and Conditions");
            addAnchor(document, "CW_WAIVER");
            addAnchor(document, "CW_SCOPE");
            addAnchor(document, "CW_CONDITIONS");
            insertSectionBreak(document);
            addSectionTitle(document, "Reservation and Closing");
            addAnchor(document, "CW_RESERVATION");
            addAnchor(document, "CW_GOVERNING_LAW");
            addAnchor(document, "CW_SIGNATURE");

            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static String readSql(Path sqlDir) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> files = Files.list(sqlDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            sb.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n');
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
        }
        return sb.toString();
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
