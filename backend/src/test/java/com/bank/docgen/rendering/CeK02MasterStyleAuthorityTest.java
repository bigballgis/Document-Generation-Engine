package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParser;
import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParserTest;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

/**
 * CE-K02 rendering inheritance — BDD-CE-K02-008…011.
 */
class CeK02MasterStyleAuthorityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void styleRefAssemblesWithoutCalibriHardcodeWhenMasterHasDocDefaults() throws Exception {
        byte[] master = MasterDocxStyleCatalogParserTest.dualFontMasterBytes();
        DocxAssembler assembler = StructuredContentDocxWriterTestSupport.createAssembler(objectMapper);

        String binding = """
                {"nodes":[{"type":"paragraph","styleRef":"ClauseBody","children":[
                  {"type":"textRun","value":"Clause body Fangsong"}
                ]}]}
                """;
        byte[] assembled = assembler.assembleStructuredFromBytes(
                master,
                Map.of("BODY", binding),
                Map.of(),
                Map.of()
        );

        String documentXml = readPart(assembled, "word/document.xml");
        assertThat(documentXml).contains("ClauseBody");
        assertThat(documentXml).contains("Clause body Fangsong");
        assertThat(documentXml).doesNotContain("Calibri");
        assertThat(assembler.lastAssemblyFidelityWarnings()).doesNotContain("MASTER_STYLE_FALLBACK");

        MasterStyleCatalog catalog = MasterDocxStyleCatalogParser.parse(master);
        assertThat(catalog.hasDocDefaults()).isTrue();
        assertThat(catalog.find("ClauseBody").typography().eastAsia()).isEqualTo("仿宋");
    }

    @Test
    void missingDocDefaultsEmitsMasterStyleFallback() throws Exception {
        byte[] master = MasterDocxStyleCatalogParserTest.masterWithoutDocDefaults("仿宋");
        DocxAssembler assembler = StructuredContentDocxWriterTestSupport.createAssembler(objectMapper);

        String binding = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Fallback path"}
                ]}]}
                """;
        byte[] assembled = assembler.assembleStructuredFromBytes(
                master,
                Map.of("BODY", binding),
                Map.of(),
                Map.of()
        );

        assertThat(readPart(assembled, "word/document.xml")).contains("Fallback path");
        assertThat(assembler.lastAssemblyFidelityWarnings()).contains("MASTER_STYLE_FALLBACK");
    }

    @Test
    void wordCompatibilityDoesNotOverwriteMasterDocDefaultsWithCalibri() throws Exception {
        byte[] master = MasterDocxStyleCatalogParserTest.dualFontMasterBytes();
        byte[] afterBytes;
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(master));
                var output = new java.io.ByteArrayOutputStream()) {
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            afterBytes = output.toByteArray();
        }
        String stylesXml = readPart(afterBytes, "word/styles.xml");
        assertThat(stylesXml).contains("宋体");
        assertThat(stylesXml).contains("仿宋");
        // Must not rewrite all default font slots to Calibri when master defaults exist.
        assertThat(stylesXml).doesNotContain("w:ascii=\"Calibri\"");
    }

    private static String readPart(byte[] docx, String part) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (part.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("missing part " + part);
    }
}
