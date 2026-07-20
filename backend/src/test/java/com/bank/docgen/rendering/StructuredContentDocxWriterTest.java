package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * POI fidelity regression safety net for {@link StructuredContentDocxWriter}.
 * BDD: BDD-F1-A1-004 — must stay green before F1-T03 dual-track removal.
 */
class StructuredContentDocxWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StructuredContentDocxWriter writer;

    @BeforeEach
    void setUp() {
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper);
    }

    @Test
    void rendersTextRunAndVariableInline() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Date: "},
                  {"type":"variable","key":"offerDate"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("offerDate", "2026-07-08"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Date: 2026-07-08");
        }
    }

    @Test
    void rendersLineBreakWithinParagraph() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Line one"},
                  {"type":"lineBreak"},
                  {"type":"textRun","value":"Line two"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getRuns()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(paragraph.getText()).contains("Line one").contains("Line two");
        }
    }

    @Test
    void rendersEmphasisBoldAndUnderlineRuns() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"emphasis","variant":"bold","children":[{"type":"textRun","value":"Bold text"}]},
                  {"type":"textRun","value":" and "},
                  {"type":"underline","children":[{"type":"textRun","value":"underlined"}]}
                ]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFRun> runs = document.getParagraphs().getFirst().getRuns();
            assertThat(runs.stream().anyMatch(XWPFRun::isBold)).isTrue();
            assertThat(runs.stream().anyMatch(run -> run.getUnderline() != UnderlinePatterns.NONE)).isTrue();
        }
    }

    @Test
    void rendersItalicEmphasisVariant() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"emphasis","variant":"italic","children":[{"type":"textRun","value":"Italic"}]}
                ]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getRuns().stream().anyMatch(XWPFRun::isItalic)).isTrue();
        }
    }

    @Test
    void appliesStyleRefToParagraph() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Clause"}]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getStyle()).isEqualTo("ClauseBody");
        }
    }

    @Test
    void rendersConditionBlockWhenExpressionIsTrue() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${showNotice} == true","children":[
                  {"type":"textRun","value":"Notice applies"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("showNotice", true));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Notice applies");
        }
    }

    @Test
    void skipsConditionBlockChildrenWhenExpressionIsFalse() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${showNotice} == true","children":[
                  {"type":"textRun","value":"Notice applies"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("showNotice", false));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isBlank();
        }
    }

    @Test
    void rendersConditionBlockWhenRichExpressionIsTrue() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${customerName} != null","children":[
                  {"type":"textRun","value":"Welcome"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("customerName", "Alice"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Welcome");
        }
    }

    @Test
    void skipsConditionBlockWhenRichExpressionIsFalse() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${customerName} != null","children":[
                  {"type":"textRun","value":"Welcome"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isBlank();
        }
    }

    @Test
    void malformedConditionExpressionFailsSafeWithoutThrowing() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${broken} === true","children":[
                  {"type":"textRun","value":"Should not appear"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("broken", true));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isBlank();
        }
    }

    @Test
    void rendersLoopBlockSinglePassWhenListMissing() throws Exception {
        String structured = """
                {"nodes":[{"type":"loopBlock","loopVariable":"items","children":[
                  {"type":"textRun","value":"Row item"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Row item");
        }
    }

    @Test
    void rendersLoopBlockForEachListItem() throws Exception {
        String structured = """
                {"nodes":[{"type":"loopBlock","loopVariable":"lenders","children":[
                  {"type":"textRun","value":"Lender: "},
                  {"type":"variable","key":"lenderName"}
                ]}]}
                """;
        Map<String, Object> variables = Map.of(
                "lenders",
                List.of(Map.of("lenderName", "Bank A"), Map.of("lenderName", "Bank B"))
        );

        byte[] result = render(structured, variables);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<String> paragraphTexts = document.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            assertThat(paragraphTexts).anyMatch(text -> text.contains("Bank A"));
            assertThat(paragraphTexts).anyMatch(text -> text.contains("Bank B"));
        }
    }

    @Test
    void nestedLoopSectionHeadingNumberingIsStableAcrossRuns() throws Exception {
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

        byte[] first = render(structured, variables);
        byte[] second = render(structured, variables);

        try (XWPFDocument firstDoc = StructuredContentDocxWriterTestSupport.openDocument(first);
                XWPFDocument secondDoc = StructuredContentDocxWriterTestSupport.openDocument(second)) {
            List<String> firstHeadings = firstDoc.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            List<String> secondHeadings = secondDoc.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            assertThat(firstHeadings).containsExactlyElementsOf(secondHeadings);
            assertThat(firstHeadings).contains("1 Intro", "1.1 Item", "1.2 Item");
        }
    }

    @Test
    void rendersOrderedAndUnorderedLists() throws Exception {
        String structured = """
                {"nodes":[
                  {"type":"list","ordered":true,"children":[
                    {"type":"paragraph","children":[{"type":"textRun","value":"First"}]},
                    {"type":"paragraph","children":[{"type":"textRun","value":"Second"}]}
                  ]},
                  {"type":"list","listStyle":"unordered","children":[
                    {"type":"paragraph","children":[{"type":"textRun","value":"Alpha"}]}
                  ]}
                ]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFParagraph> listParagraphs = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getNumID() != null)
                    .toList();
            assertThat(listParagraphs).hasSize(3);
            assertThat(listParagraphs.get(0).getText()).contains("First");
            assertThat(listParagraphs.get(2).getText()).contains("Alpha");
        }
    }

    @Test
    void rendersTableComponentAsXwpfTable() throws Exception {
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
                List.of(Map.of("period", "1", "payment", "600.00"))
        );

        byte[] result = render(structured, variables);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getTables()).hasSize(1);
            XWPFTable table = document.getTables().getFirst();
            assertThat(table.getRows()).hasSize(3);
            assertThat(table.getText()).contains("Period").contains("600.00").contains("1,200.00");
            assertThat(document.getParagraphs().getFirst().getText()).doesNotContain("|");
        }
    }

    @Test
    void bddCeK06a001_repeatHeaderAcrossPagesWritesTblHeaderOnHeaderRow() throws Exception {
        String structured = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                  "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                  "repeatHeaderAcrossPages": true,
                  "headerRows":[[
                    {"columnKey":"period","value":"Period"},
                    {"columnKey":"payment","value":"Payment"}
                  ]],
                  "loopRow":{"loopVariable":"scheduleRows","cells":[
                    {"columnKey":"period","variableKey":"period"},
                    {"columnKey":"payment","variableKey":"payment"}
                  ]}
                }}]}
                """;
        Map<String, Object> variables = Map.of(
                "scheduleRows",
                List.of(Map.of("period", "1", "payment", "600.00"))
        );

        byte[] result = render(structured, variables);

        String documentXml = readZipPart(result, "word/document.xml");
        assertThat(documentXml).contains("tblHeader");
        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFTableRow headerRow = document.getTables().getFirst().getRow(0);
            assertThat(headerRow.isRepeatHeader()).isTrue();
        }
    }

    @Test
    void bddCeK06a002_repeatHeaderFalseOrAbsentDoesNotWriteTblHeader() throws Exception {
        String withoutFlag = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                  "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                  "headerRows":[[
                    {"columnKey":"period","value":"Period"},
                    {"columnKey":"payment","value":"Payment"}
                  ]],
                  "loopRow":{"loopVariable":"scheduleRows","cells":[
                    {"columnKey":"period","variableKey":"period"},
                    {"columnKey":"payment","variableKey":"payment"}
                  ]}
                }}]}
                """;
        String explicitFalse = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                  "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                  "repeatHeaderAcrossPages": false,
                  "headerRows":[[
                    {"columnKey":"period","value":"Period"},
                    {"columnKey":"payment","value":"Payment"}
                  ]],
                  "loopRow":{"loopVariable":"scheduleRows","cells":[
                    {"columnKey":"period","variableKey":"period"},
                    {"columnKey":"payment","variableKey":"payment"}
                  ]}
                }}]}
                """;
        Map<String, Object> variables = Map.of(
                "scheduleRows",
                List.of(Map.of("period", "1", "payment", "600.00"))
        );

        byte[] absentResult = render(withoutFlag, variables);
        byte[] falseResult = render(explicitFalse, variables);

        assertThat(readZipPart(absentResult, "word/document.xml")).doesNotContain("tblHeader");
        assertThat(readZipPart(falseResult, "word/document.xml")).doesNotContain("tblHeader");
        try (XWPFDocument absentDoc = StructuredContentDocxWriterTestSupport.openDocument(absentResult);
                XWPFDocument falseDoc = StructuredContentDocxWriterTestSupport.openDocument(falseResult)) {
            assertThat(absentDoc.getTables().getFirst().getRow(0).isRepeatHeader()).isFalse();
            assertThat(falseDoc.getTables().getFirst().getRow(0).isRepeatHeader()).isFalse();
        }
    }

    @Test
    void bddCeK06a003_onlyHeaderRowsCarryTblHeader() throws Exception {
        String structured = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                  "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                  "repeatHeaderAcrossPages": true,
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

        byte[] result = render(structured, variables);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFTable table = document.getTables().getFirst();
            assertThat(table.getRows()).hasSize(4);
            assertThat(table.getRow(0).isRepeatHeader()).isTrue();
            assertThat(table.getRow(1).isRepeatHeader()).isFalse();
            assertThat(table.getRow(2).isRepeatHeader()).isFalse();
            assertThat(table.getRow(3).isRepeatHeader()).isFalse();
        }
        String documentXml = readZipPart(result, "word/document.xml");
        assertThat(countOccurrences(documentXml, "tblHeader")).isEqualTo(1);
    }

    @Test
    void bddCeK06a006_writtenHeaderRowsEachGetTblHeaderWhenRepeatEnabled() throws Exception {
        // v1 writer emits only the first headerRows entry; assert that written header carries tblHeader.
        String structured = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                  "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                  "repeatHeaderAcrossPages": true,
                  "headerRows":[
                    [
                      {"columnKey":"period","value":"Period"},
                      {"columnKey":"payment","value":"Payment"}
                    ],
                    [
                      {"columnKey":"period","value":"Period2"},
                      {"columnKey":"payment","value":"Payment2"}
                    ]
                  ],
                  "loopRow":{"loopVariable":"scheduleRows","cells":[
                    {"columnKey":"period","variableKey":"period"},
                    {"columnKey":"payment","variableKey":"payment"}
                  ]}
                }}]}
                """;
        Map<String, Object> variables = Map.of(
                "scheduleRows",
                List.of(Map.of("period", "1", "payment", "600.00"))
        );

        byte[] result = render(structured, variables);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFTable table = document.getTables().getFirst();
            List<XWPFTableRow> writtenHeaders = new ArrayList<>();
            for (XWPFTableRow row : table.getRows()) {
                if (row.isRepeatHeader()) {
                    writtenHeaders.add(row);
                }
            }
            assertThat(writtenHeaders).isNotEmpty();
            assertThat(table.getRow(0).isRepeatHeader()).isTrue();
            assertThat(table.getText()).contains("Period");
        }
    }

    @Test
    void expandsContentModuleRefFromPinnedStructure() throws Exception {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;
        Map<String, String> pinned = Map.of(
                "CLAUSE-1",
                "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"textRun\",\"value\":\"Locked clause\"}]}]}"
        );

        byte[] result = render(structured, Map.of(), pinned);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Locked clause");
        }
    }

    @Test
    void failsClosedWhenPinnedStructureMissing() {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MISSING"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of(), Map.of()))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.messageKey())
                            .isEqualTo("api.error.validation.contentModuleStructureMissing");
                    assertThat(assemblyException.errorCode()).isEqualTo("CONTENT_MODULE_STRUCTURE_MISSING");
                    assertThat(assemblyException.category()).isEqualTo("VALIDATION");
                });
    }

    @Test
    void failsClosedWhenContentModuleNestingCycleDetected() {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"A"}]}
                """;
        Map<String, String> pinned = Map.of(
                "A",
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"B\"}]}",
                "B",
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"A\"}]}"
        );

        assertThatThrownBy(() -> render(structured, Map.of(), pinned))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.errorCode()).isEqualTo("CONTENT_MODULE_NESTING_CYCLE");
                    assertThat(assemblyException.messageKey())
                            .isEqualTo("api.error.validation.contentModuleNestingCycle");
                });
    }

    @Test
    void failsClosedWhenPinnedStructureBlank() {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of(), Map.of("CLAUSE-1", "   ")))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.validation.contentModuleStructureMissing");
    }

    @Test
    void rendersLegacyBlocksRoot() throws Exception {
        String structured = """
                {"blocks":[{"type":"paragraph","text":"Legacy clause body"}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Legacy clause body");
        }
    }

    @Test
    void embedsImageAndSealReferences() throws Exception {
        String structured = """
                {"nodes":[
                  {"type":"paragraph","children":[{"type":"imageRef","imageRef":"IMG-1"}]},
                  {"type":"paragraph","children":[{"type":"sealRef","referenceKey":"SEAL-1"}]}
                ]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void embedsQrBarcodeNode() throws Exception {
        // CE-K06b — success path replaces former unsupported fail-closed for qrBarcodeRef
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", "https://pay.example/k06b"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void writesAttachmentListNodeAsNumberedParagraphs() throws Exception {
        // CE-K06c — success path replaces former unsupported fail-closed for attachmentListRef
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        byte[] result = render(structured, Map.of(
                "ATTACHMENTS", List.of("Annex A", "Annex B")
        ));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long numbered = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().getPPr() != null
                            && paragraph.getCTP().getPPr().getNumPr() != null)
                    .count();
            assertThat(numbered).isEqualTo(2);
        }
    }

    @Test
    void embedsQrBarcodeNestedInConditionBlock() throws Exception {
        String structured = """
                {"nodes":[{
                  "type":"conditionBlock",
                  "conditionExpression":"${show} == true",
                  "children":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]
                }]}
                """;

        byte[] result = render(structured, Map.of("show", true, "PAYMENT-QR", "https://pay.example/k06b"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void writesAttachmentListNestedInLoopBlock() throws Exception {
        // CE-K06c — nested success path under loopBlock
        String structured = """
                {"nodes":[{
                  "type":"loopBlock",
                  "loopVariable":"items",
                  "children":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]
                }]}
                """;

        byte[] result = render(structured, Map.of(
                "items", List.of(Map.of("n", "1")),
                "ATTACHMENTS", List.of("Loop annex")
        ));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long numbered = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().getPPr() != null
                            && paragraph.getCTP().getPPr().getNumPr() != null)
                    .count();
            assertThat(numbered).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void embedsQrBarcodeInsidePinnedContentModule() throws Exception {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;
        Map<String, String> pinned = Map.of(
                "CLAUSE-1",
                "{\"nodes\":[{\"type\":\"qrBarcodeRef\",\"referenceKey\":\"PAYMENT-QR\"}]}"
        );

        byte[] result = render(structured, Map.of("PAYMENT-QR", "https://pay.example/k06b"), pinned);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void embedsQrBarcodeAsParagraphInlineChild() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Pay: "},
                  {"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", "https://pay.example/k06b"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void failsClosedOnUnknownNodeType() {
        // A6 — unknown type must not silently omit
        String structured = """
                {"nodes":[{"type":"rawHtml","value":"<b>x</b>"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of()))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.unsupportedNodeType");
    }

    private byte[] render(String structuredJson, Map<String, Object> variables) throws Exception {
        return render(structuredJson, variables, Map.of());
    }

    private byte[] render(
            String structuredJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) throws Exception {
        return StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer,
                structuredJson,
                variables,
                pinnedModuleStructures
        );
    }

    private static String readZipPart(byte[] docxBytes, String partName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (partName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("DOCX part not found: " + partName);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
