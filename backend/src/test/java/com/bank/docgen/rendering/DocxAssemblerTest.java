package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxAssemblerTest {

    private final DocxAssembler assembler = new DocxAssembler(new ObjectMapper());

    @Test
    void assemblesDocxByReplacingAnchorPlaceholder() throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            var paragraph = document.createParagraph();
            var run = paragraph.createRun();
            run.setText("Hello {{anchor:HEADER}} end");
            document.write(output);
            master = output.toByteArray();
        }

        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"text","value":"World "},{"type":"variable","key":"name"}]}]}
                """;
        Map<String, String> bindings = Map.of("HEADER", structured);
        Map<String, Object> variables = Map.of("name", "Alice");

        byte[] result = assembler.assembleFromBytes(master, assembler.buildAnchorReplacements(bindings, variables));

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            assertThat(document.getParagraphs().getFirst().getText()).contains("World Alice");
        }
    }

    @Test
    void rendersPinnedContentModuleReferenceFromLockedVersion() {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;
        Map<String, String> pinned = Map.of(
                "CLAUSE-1",
                "{\"nodes\":[{\"type\":\"text\",\"value\":\"Locked v1.0 clause\"}]}"
        );

        String rendered = assembler.renderStructuredContent(structured, Map.of(), pinned);

        assertThat(rendered).isEqualTo("Locked v1.0 clause");
    }

    @Test
    void rendersLegacyBlocksRootAsNodes() {
        String structured = """
                {"blocks":[{"type":"paragraph","text":"Legacy clause body"}]}
                """;

        String rendered = assembler.renderStructuredContent(structured, Map.of());

        assertThat(rendered).isEqualTo("Legacy clause body");
    }

    @Test
    void rendersConditionBlockWhenSimpleExpressionIsTrue() {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${showNotice} == true","children":[{"type":"text","value":"Notice applies"}]}]}
                """;

        String rendered = assembler.renderStructuredContent(structured, Map.of("showNotice", true));

        assertThat(rendered).isEqualTo("Notice applies");
    }

    @Test
    void skipsConditionBlockChildrenWhenExpressionIsFalse() {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${showNotice} == true","children":[{"type":"text","value":"Notice applies"}]}]}
                """;

        String rendered = assembler.renderStructuredContent(structured, Map.of("showNotice", false));

        assertThat(rendered).isEmpty();
    }

    @Test
    void rendersLoopBlockChildrenWithSinglePassWhenListMissing() {
        String structured = """
                {"nodes":[{"type":"loopBlock","loopVariable":"items","children":[{"type":"text","value":"Row item"}]}]}
                """;

        String rendered = assembler.renderStructuredContent(structured, Map.of());

        assertThat(rendered).isEqualTo("Row item");
    }

    @Test
    void rendersTextRunLineBreakAndVariableSpacing() {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Date: "},
                  {"type":"variable","key":"offerDate"},
                  {"type":"lineBreak"},
                  {"type":"textRun","value":"To: "},
                  {"type":"variable","key":"borrowerLegalName"}
                ]}]}
                """;

        String rendered = assembler.renderStructuredContent(
                structured,
                Map.of("offerDate", "2026-07-01", "borrowerLegalName", "Pacific Rim Holdings Ltd.")
        );

        assertThat(rendered).isEqualTo("Date: 2026-07-01\nTo: Pacific Rim Holdings Ltd.");
    }

    @Test
    void rendersLoopBlockForEachListItem() {
        String structured = """
                {"nodes":[{"type":"loopBlock","loopVariable":"lenders","children":[
                  {"type":"textRun","value":"Lender: "},
                  {"type":"variable","key":"lenderName"},
                  {"type":"lineBreak"}
                ]}]}
                """;
        Map<String, Object> variables = Map.of(
                "lenders",
                List.of(
                        Map.of("lenderName", "Bank A"),
                        Map.of("lenderName", "Bank B")
                )
        );

        String rendered = assembler.renderStructuredContent(structured, variables);

        assertThat(rendered).contains("Lender: Bank A").contains("Lender: Bank B");
    }

    @Test
    void rendersTableComponentFromLoopVariable() {
        String structured = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                  "columnSchema":[{"columnKey":"name"},{"columnKey":"amount"}],
                  "headerRows":[[{"columnKey":"name","value":"Lender"},{"columnKey":"amount","value":"Commitment"}]],
                  "loopRow":{"loopVariable":"lenders","cells":[
                    {"columnKey":"name","variableKey":"lenderName"},
                    {"columnKey":"amount","variableKey":"lenderCommitment"}
                  ]},
                  "footerRows":[[{"columnKey":"name","value":"Total"},{"columnKey":"amount","variableKey":"totalCommitments"}]]
                }}]}
                """;
        Map<String, Object> variables = Map.of(
                "totalCommitments", "100",
                "lenders",
                List.of(Map.of("lenderName", "Bank A", "lenderCommitment", "60"))
        );

        String rendered = assembler.renderStructuredContent(structured, variables);

        assertThat(rendered).contains("Lender | Commitment").contains("Bank A | 60").contains("Total | 100");
    }

    @Test
    void separatesLegacyBlocksWithBlankLines() {
        String structured = """
                {"blocks":[
                  {"type":"paragraph","text":"First paragraph."},
                  {"type":"paragraph","text":"Second paragraph."}
                ]}
                """;

        String rendered = assembler.renderStructuredContent(structured, Map.of());

        assertThat(rendered).isEqualTo("First paragraph.\n\nSecond paragraph.");
    }

    @Test
    void expandsAnchorReplacementIntoMultipleBodyParagraphs() throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("{{anchor:BODY}}");
            document.write(output);
            master = output.toByteArray();
        }

        byte[] result = assembler.assembleFromBytes(master, Map.of("BODY", "First block\n\nSecond block"));

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            assertThat(document.getParagraphs()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(document.getParagraphs().get(0).getText()).isEqualTo("First block");
            assertThat(document.getParagraphs().get(1).getText()).isEqualTo("Second block");
        }
    }

    @Test
    void writeParagraphTextPreservesMultilineContentInSingleRun() throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            var paragraph = document.createParagraph();
            paragraph.createRun().setText("{{anchor:BODY}}");
            document.write(output);
            master = output.toByteArray();
        }

        byte[] result = assembler.assembleFromBytes(master, Map.of("BODY", "Line one\nLine two\nLine three"));

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Line one\nLine two\nLine three");
        }
    }

    @Test
    void removesMasterLayoutFillerParagraphDuringAssembly() throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            var filler = document.createParagraph().createRun();
            filler.setText("Section-level anchor in the master layout container. Long-form clause text.");
            var anchor = document.createParagraph().createRun();
            anchor.setText("{{anchor:BODY}}");
            document.write(output);
            master = output.toByteArray();
        }

        byte[] result = assembler.assembleFromBytes(master, Map.of("BODY", "Rendered clause body"));

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            assertThat(document.getParagraphs()).hasSize(1);
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Rendered clause body");
        }
    }

    @Test
    void assembledDocxContainsWordStylesWithDocDefaultsAndNormal() throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("{{anchor:BODY}}");
            document.write(output);
            master = output.toByteArray();
        }

        assertThat(containsZipEntry(master, "word/styles.xml")).isFalse();

        byte[] result = assembler.assembleFromBytes(master, Map.of("BODY", "Visible preview text"));

        assertThat(containsZipEntry(result, "word/styles.xml")).isTrue();
        String stylesXml = readZipEntryText(result, "word/styles.xml");
        assertThat(stylesXml).contains("docDefaults");
        assertThat(stylesXml).contains("Normal");
    }

    @Test
    void assembledDocxPreservesPackagePartsFromMaster() throws Exception {
        Path masterPath = Path.of("..", "deploy", "demo-fol", "assets", "wholesale-fol-master.docx");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(masterPath), "FOL demo master asset missing");
        byte[] master = Files.readAllBytes(masterPath);
        byte[] result = assembler.assembleFromBytes(master, Map.of("FOL_HEADER", "Sample header"));

        assertThat(containsZipEntry(result, "word/document.xml")).isTrue();
        assertThat(containsZipEntry(result, "word/header1.xml")).isTrue();
        assertThat(containsZipEntry(result, "word/footer1.xml")).isTrue();
        assertThat(readZipEntryText(result, "word/document.xml")).contains("Sample header");
    }

    private static String readZipEntryText(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Missing zip entry: " + entryName);
    }

    private static boolean containsZipEntry(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
