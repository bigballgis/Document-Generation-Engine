package com.bank.docgen.rendering.goldencorpus;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxPdfConversionPreprocessor;
import com.bank.docgen.rendering.LibreOfficePdfConversionService;
import com.bank.docgen.rendering.PdfConversionOptions;
import com.bank.docgen.rendering.PdfConversionPoolRejectionMetrics;
import com.bank.docgen.rendering.PdfConversionPostProcessor;
import com.bank.docgen.rendering.PdfEncryptionService;
import com.bank.docgen.rendering.StructuredContentDocxWriterTestSupport;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.compute.ComputeVariableDefinition;
import com.bank.docgen.sharedkernel.document.compute.VariableComputeEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assumptions;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Runs ACTIVE golden-corpus packages: assemble DOCX, assert keypaths, optionally PDF.
 */
public final class GoldenCorpusActiveRunner {

    private final ObjectMapper objectMapper;
    private final GoldenCorpusAssertionLoader assertionLoader;
    private final GoldenCorpusDocxAssertor docxAssertor;
    private final GoldenCorpusPdfAssertor pdfAssertor;
    private final DocxAssembler assembler;
    private final PdfEncryptionService pdfEncryptionService;

    public GoldenCorpusActiveRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.assertionLoader = new GoldenCorpusAssertionLoader(objectMapper);
        this.docxAssertor = new GoldenCorpusDocxAssertor();
        this.pdfAssertor = new GoldenCorpusPdfAssertor();
        this.assembler = StructuredContentDocxWriterTestSupport.createAssembler(objectMapper);
        this.pdfEncryptionService = new PdfEncryptionService();
    }

    public void runActivePackages(List<GoldenCorpusPackage> packages) throws Exception {
        for (GoldenCorpusPackage corpusPackage : packages) {
            if (corpusPackage.maturity() != GoldenCorpusMaturity.ACTIVE) {
                continue;
            }
            runOne(corpusPackage);
        }
    }

    public void runOne(GoldenCorpusPackage corpusPackage) throws Exception {
        JsonNode template = readJson(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.INPUT_TEMPLATE));
        JsonNode variablesNode = readJson(
                corpusPackage.directory().resolve(GoldenCorpusPackageLayout.INPUT_VARIABLES)
        );
        Map<String, String> bindings = extractBindings(template);
        Map<String, Object> variables = objectMapper.convertValue(
                variablesNode,
                new TypeReference<Map<String, Object>>() {
                }
        );
        if (variables == null) {
            variables = Map.of();
        }
        variables = applyComputeIfPresent(template, variables);

        byte[] masterBytes = Files.readAllBytes(
                corpusPackage.directory().resolve(GoldenCorpusPackageLayout.INPUT_MASTER)
        );
        byte[] assembled = assembler.assembleStructuredFromBytes(
                masterBytes,
                bindings,
                variables,
                Map.of()
        );

        JsonNode docxAssertions = assertionLoader.loadDocxAssertions(corpusPackage.directory());
        docxAssertor.assertDocx(assembled, docxAssertions);

        JsonNode pdfAssertions = assertionLoader.loadPdfAssertions(corpusPackage.directory());
        EncryptionOptionsView encryption = extractEncryption(template);
        runPdfHalf(corpusPackage, assembled, pdfAssertions, encryption);
    }

    private void runPdfHalf(
            GoldenCorpusPackage corpusPackage,
            byte[] assembledDocx,
            JsonNode pdfAssertions,
            EncryptionOptionsView encryption
    ) throws Exception {
        if (pdfAssertions.path("deferred").asBoolean(false)) {
            return;
        }
        boolean hasAssertions = pdfAssertions.path("assertions").isArray()
                && !pdfAssertions.path("assertions").isEmpty();
        boolean requireEncrypted = pdfAssertions.path("requireEncrypted").asBoolean(false);
        if (!hasAssertions && !requireEncrypted) {
            return;
        }

        String pdfSource = corpusPackage.manifest().pdfSource() == null
                ? "LIBREOFFICE"
                : corpusPackage.manifest().pdfSource();
        byte[] plainPdf;
        if ("SYNTHETIC".equalsIgnoreCase(pdfSource)) {
            plainPdf = synthesizePdfFromDocxText(assembledDocx);
        } else {
            String soffice = System.getenv().getOrDefault("LIBREOFFICE_COMMAND", "soffice");
            Assumptions.assumeTrue(
                    isSofficeAvailable(soffice),
                    "Skipping PDF assertions for package '" + corpusPackage.id()
                            + "': LibreOffice soffice unavailable (DOCX assertions already executed)"
            );
            plainPdf = convertWithLibreOffice(assembledDocx, soffice);
        }

        if (encryption != null && Boolean.TRUE.equals(encryption.enabled())) {
            byte[] encrypted = pdfEncryptionService.encrypt(plainPdf, encryption);
            pdfAssertor.assertEncryptedPdf(encrypted, encryption.openPassword(), pdfAssertions);
        } else {
            pdfAssertor.assertPlainPdf(plainPdf, pdfAssertions);
        }
    }

    private byte[] synthesizePdfFromDocxText(byte[] docxBytes) throws IOException {
        String xml = GoldenCorpusDocxAssertor.readZipPartAsString(docxBytes, "word/document.xml");
        String text = xml.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (text.length() > 200) {
            text = text.substring(0, 200);
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 750);
                content.showText(sanitizeForPdf(text));
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static String sanitizeForPdf(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            if (ch >= 32 && ch < 127) {
                builder.append(ch);
            } else {
                builder.append(' ');
            }
        }
        String sanitized = builder.toString().trim();
        return sanitized.isEmpty() ? "golden-corpus" : sanitized;
    }

    private byte[] convertWithLibreOffice(byte[] docxBytes, String soffice) {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setLibreOfficeCommand(soffice);
        properties.setConversionTimeoutSeconds(120);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(2);
        executor.setThreadNamePrefix("golden-corpus-pdf-");
        executor.initialize();
        try {
            LibreOfficePdfConversionService conversionService = new LibreOfficePdfConversionService(
                    properties,
                    CircuitBreakerRegistry.ofDefaults(),
                    RetryRegistry.ofDefaults(),
                    executor,
                    new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor()),
                    new PdfConversionPoolRejectionMetrics(new SimpleMeterRegistry())
            );
            return conversionService
                    .convertWithResult(docxBytes, PdfConversionOptions.stampingDisabled())
                    .pdfBytes();
        } finally {
            executor.shutdown();
        }
    }

    private Map<String, Object> applyComputeIfPresent(JsonNode template, Map<String, Object> variables) {
        JsonNode schemas = template.get("variableSchemas");
        if (schemas == null || !schemas.isArray() || schemas.isEmpty()) {
            return variables;
        }
        List<ComputeVariableDefinition> definitions = new ArrayList<>();
        for (JsonNode schema : schemas) {
            String key = schema.path("variableKey").asText(null);
            if (key == null || key.isBlank()) {
                continue;
            }
            String type = schema.path("variableType").asText("");
            String expression = schema.path("computeExpression").asText(null);
            definitions.add(new ComputeVariableDefinition(
                    key,
                    expression,
                    "COMPUTED".equalsIgnoreCase(type)
            ));
        }
        String locale = template.path("locale").asText(null);
        return VariableComputeEngine.INSTANCE.evaluateAll(definitions, variables, locale);
    }

    private Map<String, String> extractBindings(JsonNode template) {
        JsonNode bindingsNode = template.path("bindings");
        if (!bindingsNode.isObject()) {
            throw new GoldenCorpusException("template.json bindings object is required for ACTIVE packages");
        }
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

    private EncryptionOptionsView extractEncryption(JsonNode template) {
        JsonNode encryption = template.get("encryption");
        if (encryption == null || encryption.isNull()) {
            return null;
        }
        Boolean enabled = encryption.path("enabled").isMissingNode()
                ? null
                : encryption.path("enabled").asBoolean();
        String openPassword = textOrNull(encryption, "openPassword");
        String ownerPassword = textOrNull(encryption, "ownerPassword");
        List<String> permissions = null;
        if (encryption.path("permissions").isArray()) {
            permissions = objectMapper.convertValue(
                    encryption.get("permissions"),
                    new TypeReference<List<String>>() {
                    }
            );
        }
        return new EncryptionOptionsView(enabled, openPassword, ownerPassword, permissions);
    }

    private JsonNode readJson(java.nio.file.Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return objectMapper.readTree(input);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }

    private static boolean isSofficeAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
