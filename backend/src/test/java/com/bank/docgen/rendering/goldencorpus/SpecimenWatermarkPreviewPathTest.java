package com.bank.docgen.rendering.goldencorpus;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxSpecimenWatermarkStamper;
import com.bank.docgen.rendering.PdfSpecimenWatermarkStamper;
import com.bank.docgen.rendering.StructuredContentDocxWriterTestSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-G02-GOLD-002 — preview watermark path vs formal zero-watermark.
 */
class SpecimenWatermarkPreviewPathTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GoldenCorpusScanner scanner = new GoldenCorpusScanner(OBJECT_MAPPER);
    private final DocxAssembler assembler = StructuredContentDocxWriterTestSupport.createAssembler(OBJECT_MAPPER);

    @Test
    void previewStampAddsSpecimenWhileFormalAssembleDoesNot() throws Exception {
        GoldenCorpusPackage corpusPackage = scanner.scanAndValidate().stream()
                .filter(pkg -> "specimen-watermark".equals(pkg.id()))
                .findFirst()
                .orElseThrow();

        JsonNode template = OBJECT_MAPPER.readTree(
                Files.readString(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.INPUT_TEMPLATE))
        );
        Map<String, String> bindings = extractBindings(template);
        Map<String, Object> variables = OBJECT_MAPPER.convertValue(
                OBJECT_MAPPER.readTree(
                        Files.readString(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.INPUT_VARIABLES))
                ),
                new TypeReference<Map<String, Object>>() {
                }
        );
        if (variables == null) {
            variables = Map.of();
        }
        byte[] masterBytes = Files.readAllBytes(
                corpusPackage.directory().resolve(GoldenCorpusPackageLayout.INPUT_MASTER)
        );

        byte[] formal = assembler.assembleStructuredFromBytes(masterBytes, bindings, variables, Map.of());
        assertThat(anyZipPartContains(formal, "word/header", "SPECIMEN")).isFalse();
        assertThat(anyZipPartContains(formal, "word/footer", "SPECIMEN")).isFalse();
        assertThat(readZipPart(formal, "word/document.xml")).doesNotContain("SPECIMEN");

        byte[] previewDocx = DocxSpecimenWatermarkStamper.apply(formal);
        assertThat(anyZipPartContains(previewDocx, "word/header", "SPECIMEN")).isTrue();
        assertThat(anyZipPartContains(previewDocx, "word/footer", "SPECIMEN")).isTrue();

        byte[] syntheticPdf = synthesizePdf("Specimen watermark body page");
        byte[] previewPdf = PdfSpecimenWatermarkStamper.apply(syntheticPdf);
        try (PDDocument document = Loader.loadPDF(previewPdf)) {
            assertThat(new PDFTextStripper().getText(document).replaceAll("\\s+", "")).contains("SPECIMEN");
        }
        try (PDDocument document = Loader.loadPDF(syntheticPdf)) {
            assertThat(new PDFTextStripper().getText(document)).doesNotContain("SPECIMEN");
        }
    }

    private static Map<String, String> extractBindings(JsonNode template) {
        JsonNode bindingsNode = template.path("bindings");
        Map<String, String> bindings = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = bindingsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                bindings.put(entry.getKey(), value.asText());
            } else {
                bindings.put(entry.getKey(), value.toString());
            }
        }
        return bindings;
    }

    private static byte[] synthesizePdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 750);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static boolean anyZipPartContains(byte[] docxBytes, String namePrefix, String substring) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith(namePrefix) && name.endsWith(".xml")) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    if (xml.contains(substring)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        throw new AssertionError("Missing zip part: " + partName);
    }
}
