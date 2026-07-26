package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

class DocxAssemblerTest {

    private final DocxAssembler assembler = StructuredContentDocxWriterTestSupport.createAssembler(new ObjectMapper());

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
    void embedsQrBarcodeNode() {
        // CE-K06b — qrBarcodeRef embeds via ZXing (no longer unsupportedNodeType)
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        String rendered = assembler.renderStructuredContent(
                structured,
                Map.of("PAYMENT-QR", "https://pay.example/k06b")
        );

        assertThat(rendered).isBlank();
    }

    @Test
    void embedsAttachmentListNode() {
        // CE-K06c — attachmentListRef writes numbered list (no longer unsupportedNodeType)
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        String rendered = assembler.renderStructuredContent(
                structured,
                Map.of("ATTACHMENTS", java.util.List.of("Annex A", "Annex B"))
        );

        assertThat(rendered).contains("Annex A").contains("Annex B");
    }

    @Test
    void failsClosedOnMissingAttachmentListPayload() {
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        assertThatThrownBy(() -> assembler.renderStructuredContent(structured, Map.of()))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.attachmentListPayloadMissing");
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

    @Test
    void bddDemoExp001_emphasisAndUnderlineRenderToWordRuns() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"emphasis","variant":"bold","children":[{"type":"textRun","value":"Bold text"}]},
                  {"type":"textRun","value":" and "},
                  {"type":"underline","children":[{"type":"textRun","value":"underlined"}]}
                ]}]}
                """;

        byte[] result = assembleStructuredAnchor("BODY", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            List<XWPFRun> runs = paragraph.getRuns();
            assertThat(runs).hasSizeGreaterThanOrEqualTo(2);
            assertThat(runs.stream().anyMatch(XWPFRun::isBold)).isTrue();
            assertThat(runs.stream().anyMatch(run -> run.getUnderline() != UnderlinePatterns.NONE)).isTrue();
            assertThat(runs).hasSizeGreaterThan(1);
        }
    }

    @Test
    void bddDemoExp002_orderedAndUnorderedListsRenderAsWordLists() throws Exception {
        String structured = """
                {"nodes":[
                  {"type":"list","ordered":true,"children":[
                    {"type":"paragraph","children":[{"type":"textRun","value":"First"}]},
                    {"type":"paragraph","children":[{"type":"textRun","value":"Second"}]},
                    {"type":"paragraph","children":[{"type":"textRun","value":"Third"}]}
                  ]},
                  {"type":"list","listStyle":"unordered","children":[
                    {"type":"paragraph","children":[{"type":"textRun","value":"Alpha"}]},
                    {"type":"paragraph","children":[{"type":"textRun","value":"Beta"}]}
                  ]}
                ]}
                """;

        byte[] result = assembleStructuredAnchor("BODY", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            List<XWPFParagraph> listParagraphs = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getNumID() != null)
                    .toList();
            assertThat(listParagraphs).hasSize(5);
            long orderedCount = listParagraphs.stream()
                    .limit(3)
                    .filter(paragraph -> paragraph.getNumFmt() != null)
                    .count();
            assertThat(orderedCount).isEqualTo(3);
            assertThat(listParagraphs.get(0).getText()).contains("First");
            assertThat(listParagraphs.get(3).getText()).contains("Alpha");
        }
    }

    @Test
    void bddDemoExp003_styleRefResolvesToMasterStyleCatalogEntry() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Clause text"}]}]}
                """;

        byte[] result = assembleStructuredAnchor("BODY", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            assertThat(document.getParagraphs().getFirst().getStyle()).isEqualTo("ClauseBody");
            assertThat(readZipEntryText(result, "word/styles.xml")).contains("ClauseBody");
        }
    }

    @Test
    void bddDemoExp004_tableComponentRendersAsXwpfTable() throws Exception {
        String structured = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                  "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                  "headerRows":[[
                    {"columnKey":"period","value":"Period"},
                    {"columnKey":"payment","value":"Payment"}
                  ]],
                  "loopRow":{"loopVariable":"scheduleRows","cells":[
                    {"columnKey":"period","variableKey":"period"},
                    {"columnKey":"payment","variableKey":"payment"}
                  ]},
                  "footerRows":[[
                    {"columnKey":"period","value":"Total"},
                    {"columnKey":"payment","variableKey":"totalPayment"}
                  ]]
                }}]}
                """;
        Map<String, Object> variables = Map.of(
                "totalPayment", "1,200.00",
                "scheduleRows",
                List.of(
                        Map.of("period", "1", "payment", "600.00"),
                        Map.of("period", "2", "payment", "600.00")
                )
        );

        byte[] result = assembleStructuredAnchor("BODY", structured, variables);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            assertThat(document.getTables()).hasSize(1);
            XWPFTable table = document.getTables().getFirst();
            assertThat(table.getRow(0).getTableCells()).hasSize(2);
            assertThat(table.getRows()).hasSize(4);
            assertThat(table.getText()).contains("Period").contains("600.00").contains("1,200.00");
            assertThat(document.getParagraphs().getFirst().getText()).doesNotContain("|");
        }
    }

    @Test
    void bddDemoExp013_validStylesDoNotRequireControlledStyleFallback() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Valid"}]}]}
                """;

        byte[] result = assembleStructuredAnchor("BODY", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            assertThat(document.getParagraphs().getFirst().getStyle()).isEqualTo("ClauseBody");
        }
    }

    @Test
    void bddDemoExp014_numberingStableAfterConditionAndLoopRender() throws Exception {
        String structured = """
                {
                  "nodes": [
                    { "type": "sectionHeading", "numbering": { "level": 1 }, "children": [{ "type": "textRun", "value": "Intro" }] },
                    {
                      "type": "loopBlock",
                      "loopVariable": "items",
                      "children": [
                        { "type": "sectionHeading", "numbering": { "level": 2 }, "children": [{ "type": "textRun", "value": "Item" }] }
                      ]
                    }
                  ]
                }
                """;
        Map<String, Object> variables = Map.of(
                "items",
                List.of(Map.of("name", "A"), Map.of("name", "B"))
        );

        byte[] first = assembleStructuredAnchor("BODY", structured, variables);
        byte[] second = assembleStructuredAnchor("BODY", structured, variables);

        try (XWPFDocument firstDoc = new XWPFDocument(new java.io.ByteArrayInputStream(first));
                XWPFDocument secondDoc = new XWPFDocument(new java.io.ByteArrayInputStream(second))) {
            List<String> firstHeadings = firstDoc.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            List<String> secondHeadings = secondDoc.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            assertThat(firstHeadings).containsExactlyElementsOf(secondHeadings);
            assertThat(firstHeadings).contains("1 Intro", "1.1 Item", "1.2 Item");
        }
    }

    @Test
    void bddF1A1_002_tableCellStructuredContentPreservesEmphasis() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"emphasis","variant":"bold","children":[{"type":"textRun","value":"Cell value"}]}
                ]}]}
                """;

        byte[] result = assembleStructuredTableCellAnchor("CELL", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            XWPFTableCell cell = document.getTables().getFirst().getRow(0).getCell(0);
            assertThat(cell.getParagraphs().getFirst().getRuns().stream().anyMatch(XWPFRun::isBold)).isTrue();
        }
    }

    @Test
    void bddF1A1_003_headerStructuredContentPreservesEmphasis() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"emphasis","variant":"bold","children":[{"type":"textRun","value":"Header title"}]}
                ]}]}
                """;

        byte[] result = assembleStructuredHeaderAnchor("HDR", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            XWPFHeader header = document.getHeaderList().getFirst();
            assertThat(header.getParagraphs().getFirst().getRuns().stream().anyMatch(XWPFRun::isBold)).isTrue();
        }
    }

    @Test
    void bddF1A1_003_footerStructuredContentPreservesUnderline() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"underline","children":[{"type":"textRun","value":"Footer note"}]}
                ]}]}
                """;

        byte[] result = assembleStructuredFooterAnchor("FTR", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            XWPFFooter footer = document.getFooterList().getFirst();
            assertThat(footer.getParagraphs().getFirst().getRuns().stream()
                    .anyMatch(run -> run.getUnderline() != UnderlinePatterns.NONE)).isTrue();
        }
    }

    @Test
    void bddDemoExp015_imageAndSealReferencesEmbedPictures() throws Exception {
        String structured = """
                {"nodes":[
                  {"type":"paragraph","children":[{"type":"imageRef","imageRef":"IMG-1"}]},
                  {"type":"paragraph","children":[{"type":"sealRef","referenceKey":"SEAL-1"}]}
                ]}
                """;

        byte[] result = assembleStructuredAnchor("BODY", structured);

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(2);
            assertThat(containsZipEntry(result, "word/media/image1.png")).isTrue();
        }
    }


    @Test
    void assemblesBodyAnchorWhenMasterBodyContainsTableBeforeAnchor() throws Exception {
        byte[] master = masterWithBodyTableBeforeAnchor("BODY");
        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"text","value":"REPLACED BODY"}]}]}
                """;

        byte[] result = assembler.assembleStructuredFromBytes(
                master,
                Map.of("BODY", structured),
                Map.of(),
                Map.of()
        );

        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(result))) {
            String bodyText = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
            assertThat(bodyText).contains("REPLACED BODY");
            assertThat(bodyText).doesNotContain("{{anchor:BODY}}");
            assertThat(document.getTables()).hasSize(1);
            assertThat(document.getTables().get(0).getRow(0).getCell(0).getText())
                    .isEqualTo("HEADER TABLE");
        }
    }

    private static byte[] masterWithBodyTableBeforeAnchor(String anchorId) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(1, 1);
            table.getRow(0).getCell(0).setText("HEADER TABLE");
            document.createParagraph().createRun().setText("{{anchor:" + anchorId + "}}");
            document.write(out);
            return out.toByteArray();
        }
    }

    private byte[] assembleStructuredAnchor(String anchorId, String structuredJson) throws Exception {
        return assembleStructuredAnchor(anchorId, structuredJson, Map.of());
    }

    private byte[] assembleStructuredAnchor(
            String anchorId,
            String structuredJson,
            Map<String, Object> variables
    ) throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("{{anchor:" + anchorId + "}}");
            document.write(output);
            master = output.toByteArray();
        }
        return assembler.assembleStructuredFromBytes(
                master,
                Map.of(anchorId, structuredJson),
                variables,
                Map.of()
        );
    }

    private byte[] assembleStructuredTableCellAnchor(String anchorId, String structuredJson) throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(1, 1);
            table.getRow(0).getCell(0).getParagraphs().getFirst().createRun().setText("{{anchor:" + anchorId + "}}");
            document.write(output);
            master = output.toByteArray();
        }
        return assembler.assembleStructuredFromBytes(master, Map.of(anchorId, structuredJson), Map.of(), Map.of());
    }

    private byte[] assembleStructuredHeaderAnchor(String anchorId, String structuredJson) throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("{{anchor:" + anchorId + "}}");
            document.write(output);
            master = output.toByteArray();
        }
        return assembler.assembleStructuredFromBytes(master, Map.of(anchorId, structuredJson), Map.of(), Map.of());
    }

    private byte[] assembleStructuredFooterAnchor(String anchorId, String structuredJson) throws Exception {
        byte[] master;
        try (XWPFDocument document = new XWPFDocument(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createFooter(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("{{anchor:" + anchorId + "}}");
            document.write(output);
            master = output.toByteArray();
        }
        return assembler.assembleStructuredFromBytes(master, Map.of(anchorId, structuredJson), Map.of(), Map.of());
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
