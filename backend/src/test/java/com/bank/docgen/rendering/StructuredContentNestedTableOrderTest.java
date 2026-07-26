package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

/**
 * FOS-W15-1 / W15-2 — nested table/list under conditionBlock must render (not silent-empty)
 * and multi-child block order must be preserved.
 */
class StructuredContentNestedTableOrderTest extends StructuredContentDocxWriterTestFixtures {

    @Test
    void nestedTableInsideTrueConditionRendersRows() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${showSchedule} == true","children":[
                  {"type":"tableComponentRef","tableComponentRef":"TBL-NEST","tableComponent":{
                    "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                    "headerRows":[[
                      {"columnKey":"period","value":"Period"},
                      {"columnKey":"payment","value":"Payment"}
                    ]],
                    "loopRow":{"loopVariable":"scheduleRows","cells":[
                      {"columnKey":"period","variableKey":"period"},
                      {"columnKey":"payment","variableKey":"payment"}
                    ]}
                  }}
                ]}]}
                """;
        Map<String, Object> variables = Map.of(
                "showSchedule", true,
                "scheduleRows",
                List.of(Map.of("period", "1", "payment", "600.00"))
        );

        byte[] result = render(structured, variables);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getTables()).hasSize(1);
            assertThat(document.getTables().getFirst().getText())
                    .contains("Period")
                    .contains("600.00");
        }
    }

    @Test
    void nestedListInsideTrueConditionRendersItems() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${showList} == true","children":[
                  {"type":"list","ordered":true,"children":[
                    {"type":"paragraph","children":[{"type":"textRun","value":"Alpha"}]},
                    {"type":"paragraph","children":[{"type":"textRun","value":"Beta"}]}
                  ]}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("showList", true));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<XWPFParagraph> listParagraphs = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getNumID() != null)
                    .toList();
            assertThat(listParagraphs).hasSize(2);
            assertThat(listParagraphs.get(0).getText()).contains("Alpha");
            assertThat(listParagraphs.get(1).getText()).contains("Beta");
        }
    }

    @Test
    void multiChildConditionPreservesParagraphTableParagraphOrder() throws Exception {
        String structured = """
                {"nodes":[{"type":"conditionBlock","conditionExpression":"${show} == true","children":[
                  {"type":"paragraph","children":[{"type":"textRun","value":"Before table"}]},
                  {"type":"tableComponentRef","tableComponentRef":"TBL-ORD","tableComponent":{
                    "columnSchema":[{"columnKey":"col"}],
                    "headerRows":[[{"columnKey":"col","value":"Col"}]],
                    "loopRow":{"loopVariable":"rows","cells":[{"columnKey":"col","variableKey":"col"}]}
                  }},
                  {"type":"paragraph","children":[{"type":"textRun","value":"After table"}]}
                ]}]}
                """;
        Map<String, Object> variables = Map.of(
                "show", true,
                "rows", List.of(Map.of("col", "row-1"))
        );

        byte[] result = render(structured, variables);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.size()).isGreaterThanOrEqualTo(3);
            int firstPara = -1;
            int tableAt = -1;
            int lastPara = -1;
            for (int index = 0; index < body.size(); index++) {
                IBodyElement element = body.get(index);
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && text.contains("Before table") && firstPara < 0) {
                        firstPara = index;
                    }
                    if (text != null && text.contains("After table")) {
                        lastPara = index;
                    }
                } else if (element instanceof XWPFTable) {
                    tableAt = index;
                }
            }
            assertThat(firstPara).isGreaterThanOrEqualTo(0);
            assertThat(tableAt).isGreaterThan(firstPara);
            assertThat(lastPara).isGreaterThan(tableAt);
            assertThat(document.getTables().getFirst().getText()).contains("row-1");
        }
    }

    @Test
    void flatHeaderRowsShapeStillPopulatesHeaderCells() throws Exception {
        // FOS-W15-4: ConvertTo-Json flattened shape
        String structured = """
                {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-FLAT","tableComponent":{
                  "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                  "headerRows":[
                    {"columnKey":"period","value":"Period"},
                    {"columnKey":"payment","value":"Payment"}
                  ],
                  "loopRow":{"loopVariable":"scheduleRows","cells":[
                    {"columnKey":"period","variableKey":"period"},
                    {"columnKey":"payment","variableKey":"payment"}
                  ]}
                }}]}
                """;

        byte[] result = render(
                structured,
                Map.of("scheduleRows", List.of(Map.of("period", "1", "payment", "100")))
        );

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getTables().getFirst().getText())
                    .contains("Period")
                    .contains("Payment")
                    .contains("100");
        }
    }
}
