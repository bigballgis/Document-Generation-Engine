package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import com.bank.docgen.rendering.StructuredContentDocxWriter;
import com.bank.docgen.rendering.StructuredContentDocxWriterTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * FOS-W15-6 — typography gate must assert a generated letter artifact, not only the master shell.
 */
class DemoGeneratedLetterTypographyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedLetterAppliesDirectFormatSpacingAndBankStyleCatalog() throws Exception {
        byte[] generated = assembleGeneratedLetter();

        String stylesXml = DemoMasterDocxAssertions.readStylesXml(generated);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:eastAsia=\"Noto Sans CJK SC\"");
        // ClauseBody spacingAfterTwips=120 from demo-bank-style-manifest.json
        assertThat(stylesXml).contains("w:after=\"120\"");

        String documentXml = readZipPart(generated, "word/document.xml");
        assertThat(documentXml)
                .contains("Generated body with spacing")
                .contains("w:spacing")
                .contains("w:after=\"240\"");
    }

    private byte[] assembleGeneratedLetter() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxStyleSupport.applySharedBankStyles(document);
            document.createParagraph().createRun().setText("{{anchor:BODY}}");
            StructuredContentDocxWriter writer = StructuredContentDocxWriterTestSupport.createWriter(
                    objectMapper
            );
            String structured = """
                    {"nodes":[{"type":"paragraph","styleRef":"ClauseBody","directFormat":{"spacingAfter":12},"children":[
                      {"type":"textRun","value":"Generated body with spacing"}
                    ]}]}
                    """;
            writer.replaceAnchorParagraph(document, 0, structured, Map.of(), Map.of());
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static String readZipPart(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Missing zip entry: " + entryName);
    }
}
