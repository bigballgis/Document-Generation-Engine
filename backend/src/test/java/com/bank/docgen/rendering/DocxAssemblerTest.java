package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxAssemblerTest {

    private final DocxAssembler assembler = new DocxAssembler(new ObjectMapper());

    @Test
    void assemblesDocxByReplacingAnchorPlaceholder() throws Exception {
        byte[] master = """
                PK master
                """.getBytes(StandardCharsets.UTF_8);
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
}
