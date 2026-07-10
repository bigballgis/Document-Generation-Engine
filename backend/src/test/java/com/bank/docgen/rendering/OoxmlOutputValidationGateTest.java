package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * LR-A6 / CD-HARD-T03 / ADR-0043: OOXML output validation gate.
 *
 * <p>Asserts production {@link OoxmlOutputValidator} rejects structurally unsafe DOCX (fail-closed)
 * and accepts well-formed structured assembly output across corpus node families.
 *
 * <p>Residual (ADR-0043): full ECMA-376 XSD schema validation is deferred; this gate enforces
 * OPC open + XML well-formedness on Word XML parts (document/styles/numbering/headers/footers).
 */
class OoxmlOutputValidationGateTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MESSAGE_KEY = "api.error.rendering.ooxmlValidationFailed";

    private final OoxmlOutputValidator validator = new OoxmlOutputValidator();
    private final DocxAssembler assembler = StructuredContentDocxWriterTestSupport.createAssembler(OBJECT_MAPPER);

    @Test
    void rejectsCorruptedFixtureWithUnescapedAmpersand() throws Exception {
        byte[] corrupt = corruptDocxWithUnescapedAmpersandInDocumentXml();

        assertThatThrownBy(() -> validator.validate(corrupt))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.messageKey()).isEqualTo(MESSAGE_KEY);
                    assertThat(assemblyException.errorCode()).isEqualTo("OOXML_VALIDATION_FAILED");
                    assertThat(assemblyException.category()).isEqualTo("RENDERING");
                });
    }

    @Test
    void acceptsWellFormedStructuredDocxWithAdversarialText() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Bank correspondence — ampersand: A & B < C > D \\"quoted\\""},
                  {"type":"lineBreak"},
                  {"type":"variable","key":"borrowerName"}
                ]}]}
                """;

        byte[] assembled = assembler.assembleStructuredFromBytes(
                minimalMasterDocx(),
                Map.of("BODY", structured),
                Map.of("borrowerName", "Pacific Rim & Co."),
                Map.of()
        );

        validator.validate(assembled);
        assertThat(assembled).isNotEmpty();
    }

    @Test
    void acceptsWellFormedStructuredDocxWithCjkAndEmoji() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"尊敬的客户 🏦 您好 — 合同编号 "},
                  {"type":"variable","key":"contractId"}
                ]}]}
                """;

        byte[] assembled = assembler.assembleStructuredFromBytes(
                minimalMasterDocx(),
                Map.of("BODY", structured),
                Map.of("contractId", "CN-2026-☕-001"),
                Map.of()
        );

        validator.validate(assembled);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corpusNodeFamilies")
    void assembleStructuredCorpusNodeFamiliesPassValidation(String family, CorpusCase corpusCase)
            throws Exception {
        byte[] assembled = assembler.assembleStructuredFromBytes(
                minimalMasterDocx(),
                Map.of("BODY", corpusCase.structuredJson()),
                corpusCase.variables(),
                corpusCase.pinnedModules()
        );

        validator.validate(assembled);
        assertThat(assembled).isNotEmpty();
    }

    @Test
    void assembleStructuredFailsClosedWhenValidatorRejects() {
        OoxmlOutputValidator rejecting = new OoxmlOutputValidator() {
            @Override
            public void validate(byte[] docxBytes) {
                throw new DocxAssemblyException(
                        "OOXML_VALIDATION_FAILED",
                        "RENDERING",
                        MESSAGE_KEY,
                        "forced rejection"
                );
            }
        };
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setOoxmlValidationEnabled(true);
        DocxAssembler gated = new DocxAssembler(
                OBJECT_MAPPER,
                StructuredContentDocxWriterTestSupport.demoTierImageResolver(),
                rejecting,
                properties
        );

        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"ok"}]}]}
                """;

        assertThatThrownBy(() -> gated.assembleStructuredFromBytes(
                minimalMasterDocx(),
                Map.of("BODY", structured),
                Map.of(),
                Map.of()
        ))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo(MESSAGE_KEY);
    }

    @Test
    void assembleSkipsValidationWhenPropertyDisabled() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setOoxmlValidationEnabled(false);
        CountingValidator counting = new CountingValidator();
        DocxAssembler ungated = new DocxAssembler(
                OBJECT_MAPPER,
                StructuredContentDocxWriterTestSupport.demoTierImageResolver(),
                counting,
                properties
        );

        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"A & B"}]}]}
                """;

        byte[] assembled = ungated.assembleStructuredFromBytes(
                minimalMasterDocx(),
                Map.of("BODY", structured),
                Map.of(),
                Map.of()
        );

        assertThat(assembled).isNotEmpty();
        assertThat(counting.invocations).isZero();
    }

    static List<Object[]> corpusNodeFamilies() {
        return List.of(
                new Object[]{
                        "paragraph",
                        new CorpusCase(
                                """
                                        {"nodes":[{"type":"paragraph","children":[
                                          {"type":"textRun","value":"Paragraph with & < > \\"quotes\\" and 中文"}
                                        ]}]}
                                        """,
                                Map.of(),
                                Map.of()
                        )
                },
                new Object[]{
                        "list",
                        new CorpusCase(
                                """
                                        {"nodes":[
                                          {"type":"list","ordered":true,"children":[
                                            {"type":"paragraph","children":[{"type":"textRun","value":"First & item"}]},
                                            {"type":"paragraph","children":[{"type":"textRun","value":"Second <item>"}]}
                                          ]}
                                        ]}
                                        """,
                                Map.of(),
                                Map.of()
                        )
                },
                new Object[]{
                        "table",
                        new CorpusCase(
                                """
                                        {"nodes":[{"type":"tableComponentRef","tableComponentRef":"TBL-1","tableComponent":{
                                          "columnSchema":[{"columnKey":"period"},{"columnKey":"payment"}],
                                          "headerRows":[[
                                            {"columnKey":"period","value":"Period & Term"},
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
                                        """,
                                Map.of(
                                        "totalPayment", "1,200.00",
                                        "scheduleRows",
                                        List.of(
                                                Map.of("period", "1", "payment", "600.00"),
                                                Map.of("period", "2", "payment", "600.00")
                                        )
                                ),
                                Map.of()
                        )
                },
                new Object[]{
                        "module",
                        new CorpusCase(
                                """
                                        {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                                        """,
                                Map.of(),
                                Map.of(
                                        "CLAUSE-1",
                                        "{\"nodes\":[{\"type\":\"paragraph\",\"children\":"
                                                + "[{\"type\":\"textRun\",\"value\":\"Locked clause A & B\"}]}]}"
                                )
                        )
                },
                new Object[]{
                        "image",
                        new CorpusCase(
                                """
                                        {"nodes":[
                                          {"type":"paragraph","children":[{"type":"imageRef","imageRef":"IMG-1"}]}
                                        ]}
                                        """,
                                Map.of(),
                                Map.of()
                        )
                }
        );
    }

    private static byte[] minimalMasterDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var paragraph = document.createParagraph();
            var run = paragraph.createRun();
            run.setText("{{anchor:BODY}}");
            document.write(output);
            return output.toByteArray();
        }
    }

    /**
     * Builds a DOCX whose {@code word/document.xml} contains a raw {@code &} (CD-PIT-03 class).
     */
    private static byte[] corruptDocxWithUnescapedAmpersandInDocumentXml() throws Exception {
        byte[] valid;
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("SAFE_TOKEN");
            document.write(output);
            valid = output.toByteArray();
        }
        return replaceZipEntryText(valid, "word/document.xml", xml -> xml.replace("SAFE_TOKEN", "A & B"));
    }

    private static byte[] replaceZipEntryText(
            byte[] zipBytes,
            String entryName,
            java.util.function.UnaryOperator<String> transform
    ) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                entries.put(entry.getName(), zipIn.readAllBytes());
            }
        }
        byte[] original = entries.get(entryName);
        if (original == null) {
            throw new IllegalStateException("Missing zip entry: " + entryName);
        }
        String xml = new String(original, StandardCharsets.UTF_8);
        entries.put(entryName, transform.apply(xml).getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream rebuilt = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(rebuilt)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zipOut.putNextEntry(new ZipEntry(entry.getKey()));
                zipOut.write(entry.getValue());
                zipOut.closeEntry();
            }
        }
        return rebuilt.toByteArray();
    }

    private record CorpusCase(
            String structuredJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModules
    ) {
    }

    private static final class CountingValidator extends OoxmlOutputValidator {
        private int invocations;

        @Override
        public void validate(byte[] docxBytes) {
            invocations++;
            super.validate(docxBytes);
        }
    }
}
