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

class KycCddMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = "kyc-cdd-layout-v1-eight-anchors";
    private static final Path ASSET = Path.of("..", "deploy", "demo-kyc-cdd", "assets", "kyc-cdd-master.docx");
    private static final String PACKAGE_CODE = "demo-kyc-cdd";

    static final List<String> ANCHOR_IDS = List.of(
            "KYC_PARTIES",
            "KYC_PURPOSE",
            "KYC_DEFINED_TERMS",
            "KYC_REQUEST",
            "KYC_CHECKLIST",
            "KYC_CONSEQUENCES",
            "KYC_GOVERNING_LAW",
            "KYC_SIGNATURE"
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
                "Helen Cartwright",
                "Customer Due Diligence",
                "Document Checklist",
                "Meridian Retail Banking"
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
            brandRun.setText("Meridian Retail Banking — Financial Crime Compliance");
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
            addrRun.setText("1 Churchill Place, London E14 5HP  |  Meridian Retail Banking  |  FCA regulated");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            DemoMasterDocxPageNumberSupport.addDualPageNumberFields(pageLine);

            addTitle(document, "Customer Due Diligence Information Request");
            addSectionTitle(document, "Parties");
            addAnchor(document, "KYC_PARTIES");
            insertSectionBreak(document);
            addSectionTitle(document, "Purpose and Definitions");
            addAnchor(document, "KYC_PURPOSE");
            addAnchor(document, "KYC_DEFINED_TERMS");
            insertSectionBreak(document);
            addSectionTitle(document, "Request and Checklist");
            addAnchor(document, "KYC_REQUEST");
            addAnchor(document, "KYC_CHECKLIST");
            insertSectionBreak(document);
            addSectionTitle(document, "Consequences and Closing");
            addAnchor(document, "KYC_CONSEQUENCES");
            addAnchor(document, "KYC_GOVERNING_LAW");
            addAnchor(document, "KYC_SIGNATURE");

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
