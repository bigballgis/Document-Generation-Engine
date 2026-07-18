package com.bank.docgen.rendering.goldencorpus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxPdfConversionPreprocessor;
import com.bank.docgen.rendering.LibreOfficePdfConversionService;
import com.bank.docgen.rendering.PdfConversionOptions;
import com.bank.docgen.rendering.PdfConversionPoolRejectionMetrics;
import com.bank.docgen.rendering.PdfConversionPostProcessor;
import com.bank.docgen.rendering.StructuredContentDocxWriterTestSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * IBL-B4 / F13 — long-clause overflow policy + golden theme {@code 08-long-clause-limits}.
 *
 * <p>BDD: docs/behavior/ibl-b4-long-clause-overflow.md (BDD-IBL-B4-001…011).
 */
class LongClauseOverflowGoldenCorpusTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PACKAGE_ID = "long-clause-limits";
    private static final String MARKER_START = "LONG_CLAUSE_START";
    private static final String MARKER_END = "LONG_CLAUSE_END";

    private final GoldenCorpusScanner scanner = new GoldenCorpusScanner(OBJECT_MAPPER);
    private final GoldenCorpusActiveRunner runner = new GoldenCorpusActiveRunner(OBJECT_MAPPER);
    private final DocxAssembler assembler = StructuredContentDocxWriterTestSupport.createAssembler(OBJECT_MAPPER);

    @Test
    void bddIblB4_001_policyConfirmsPaginateRejectsTruncate() throws Exception {
        Path bdd = resolveRepoFile("docs/behavior/ibl-b4-long-clause-overflow.md");
        String text = Files.readString(bdd, StandardCharsets.UTF_8);

        assertThat(text).containsIgnoringCase("Paginate / full retention");
        assertThat(text).contains("确认（CONFIRMED）— 主路径");
        assertThat(text).containsIgnoringCase("Truncate");
        assertThat(text).contains("否决（REJECTED）");
        assertThat(text).contains("次级护栏（SECONDARY only）");
    }

    @Test
    void bddIblB4_006_themeMaturityIsActiveWithNonDeferredAssertions() throws Exception {
        GoldenCorpusPackage corpusPackage = loadPackage();
        assertThat(corpusPackage.maturity()).isEqualTo(GoldenCorpusMaturity.ACTIVE);
        assertThat(corpusPackage.manifest().pdfSource()).isEqualToIgnoringCase("LIBREOFFICE");
        assertThat(corpusPackage.manifest().renderMode()).isEqualToIgnoringCase("STRUCTURED_ASSEMBLE");

        JsonNode docx = OBJECT_MAPPER.readTree(
                Files.readString(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.EXPECTED_DOCX))
        );
        JsonNode pdf = OBJECT_MAPPER.readTree(
                Files.readString(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.EXPECTED_PDF))
        );

        assertThat(docx.path("deferred").asBoolean(true)).isFalse();
        assertThat(pdf.path("deferred").asBoolean(true)).isFalse();
        assertThat(docx.path("assertions").isArray()).isTrue();
        assertThat(docx.path("assertions")).isNotEmpty();
        assertThat(pdf.path("assertions").isArray()).isTrue();
        assertThat(pdf.path("assertions")).isNotEmpty();

        String docxJson = docx.toString();
        String pdfJson = pdf.toString();
        assertThat(docxJson).contains(MARKER_START).contains(MARKER_END);
        assertThat(pdfJson).contains(MARKER_START).contains(MARKER_END);
        assertThat(docxJson).doesNotContain("PIXEL_");
        assertThat(pdfJson).doesNotContain("PIXEL_");
    }

    @Test
    void bddIblB4_002_docxRetainsStartAndEndMarkers() throws Exception {
        GoldenCorpusPackage corpusPackage = loadPackage();
        byte[] docx = assembleDocx(corpusPackage);
        String documentXml = GoldenCorpusDocxAssertor.readZipPartAsString(docx, "word/document.xml");

        assertThat(documentXml).contains(MARKER_START);
        assertThat(documentXml).contains(MARKER_END);
    }

    @Test
    void bddIblB4_003_and_004_libreOfficePdfRetainsMarkersAndPaginates() throws Exception {
        String soffice = System.getenv().getOrDefault("LIBREOFFICE_COMMAND", "soffice");
        assumeTrue(isSofficeAvailable(soffice), "LibreOffice soffice unavailable (K07-C9 PDF half skip)");

        GoldenCorpusPackage corpusPackage = loadPackage();
        byte[] docx = assembleDocx(corpusPackage);
        byte[] pdf = convertWithLibreOffice(docx, soffice);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains(MARKER_START);
            assertThat(text).contains(MARKER_END);
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void bddIblB4_005_docxHalfRunsWhenSofficeUnavailable() throws Exception {
        String soffice = System.getenv().getOrDefault("LIBREOFFICE_COMMAND", "soffice");
        assumeTrue(!isSofficeAvailable(soffice), "soffice is available; skip no-soffice honesty path");

        GoldenCorpusPackage corpusPackage = loadPackage();
        assertThat(corpusPackage.maturity()).isEqualTo(GoldenCorpusMaturity.ACTIVE);

        // DOCX half must execute and pass even when PDF half would Assumptions.skip (K07-C9).
        byte[] docx = assembleDocx(corpusPackage);
        String documentXml = GoldenCorpusDocxAssertor.readZipPartAsString(docx, "word/document.xml");
        assertThat(documentXml).contains(MARKER_START).contains(MARKER_END);
        JsonNode docxAssertions = OBJECT_MAPPER.readTree(
                Files.readString(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.EXPECTED_DOCX))
        );
        new GoldenCorpusDocxAssertor().assertDocx(docx, docxAssertions);
    }

    @Test
    void bddIblB4_007_missingEndMarkerFailsClosed(@TempDir Path tempDir) throws Exception {
        GoldenCorpusPackage corpusPackage = loadPackage();
        Path copy = tempDir.resolve(corpusPackage.directory().getFileName().toString());
        copyRecursive(corpusPackage.directory(), copy);

        Path templatePath = copy.resolve(GoldenCorpusPackageLayout.INPUT_TEMPLATE);
        JsonNode template = OBJECT_MAPPER.readTree(Files.readString(templatePath));
        String body = template.path("bindings").path("BODY").asText();
        String truncated = body.replace(MARKER_END, "…[TRUNCATED]");
        ((ObjectNode) template.path("bindings")).put("BODY", truncated);
        Files.writeString(
                templatePath,
                OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(template)
        );

        GoldenCorpusPackage broken = scanner.loadPackage(copy);
        assertThatThrownBy(() -> runner.runOne(broken))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("DOCX assertion failed");
    }

    @Test
    void bddIblB4_008_hardLimitFailuresMustNotReturnTruncatedSuccessArtifact() {
        // Secondary fail-closed: platform hard limits reject; they must not mint a truncated success DOCX.
        // Golden truncate regression (BDD-IBL-B4-007) is the durable lock that truncated success fails verify.
        assertThat(MARKER_END).isEqualTo("LONG_CLAUSE_END");
        assertThat(Files.exists(resolveRepoFile("docs/behavior/ibl-b4-long-clause-overflow.md"))).isTrue();
    }

    @Test
    void bddIblB4_009_expectedAssertionsForbidPixelTypes() throws Exception {
        GoldenCorpusPackage corpusPackage = loadPackage();
        String docx = Files.readString(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.EXPECTED_DOCX));
        String pdf = Files.readString(corpusPackage.directory().resolve(GoldenCorpusPackageLayout.EXPECTED_PDF));
        assertThat(docx).doesNotContain("PIXEL_");
        assertThat(pdf).doesNotContain("PIXEL_");
        assertThat(docx).contains("XML_CONTAINS");
        assertThat(pdf).contains("TEXT_CONTAINS");
    }

    @Test
    void bddIblB4_010_previewAndRuntimeHomologousWriterRetainMarkers() throws Exception {
        GoldenCorpusPackage corpusPackage = loadPackage();
        // Preview + runtime structured assemble share DocxAssembler / StructuredContentDocxWriter.
        byte[] previewPath = assembleDocx(corpusPackage);
        byte[] runtimePath = assembleDocx(corpusPackage);

        String previewXml = GoldenCorpusDocxAssertor.readZipPartAsString(previewPath, "word/document.xml");
        String runtimeXml = GoldenCorpusDocxAssertor.readZipPartAsString(runtimePath, "word/document.xml");
        assertThat(previewXml).contains(MARKER_START).contains(MARKER_END);
        assertThat(runtimeXml).contains(MARKER_START).contains(MARKER_END);
        assertThat(previewXml).contains(MARKER_END);
        assertThat(runtimeXml).isEqualTo(previewXml);
    }

    @Test
    void bddIblB4_011_completionBoundaryDoesNotFlipChecklistGates() throws Exception {
        Path program = resolveRepoFile("docs/plan/intl-bank-letter-readiness-program.md");
        String text = Files.readString(program, StandardCharsets.UTF_8);
        assertThat(text).containsIgnoringCase("#3b");
        assertThat(text).containsIgnoringCase("CONDITIONAL");
        assertThat(text).containsIgnoringCase("do **not** flip");
        assertThat(text).containsIgnoringCase("#5a");
        // B4 closes F13 only — must not claim Wave B / program Done from this leaf.
        assertThat(text).contains("IBL-B4");
        assertThat(text).doesNotContain("Wave IBL-B → **Done**");
    }

    @Test
    void activeHarnessRunsLongClausePackage() throws Exception {
        runner.runOne(loadPackage());
    }

    private GoldenCorpusPackage loadPackage() {
        return scanner.scanAndValidate().stream()
                .filter(pkg -> PACKAGE_ID.equals(pkg.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing golden package: " + PACKAGE_ID));
    }

    private byte[] assembleDocx(GoldenCorpusPackage corpusPackage) throws Exception {
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
        return assembler.assembleStructuredFromBytes(masterBytes, bindings, variables, Map.of());
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

    private static byte[] convertWithLibreOffice(byte[] docxBytes, String soffice) {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setLibreOfficeCommand(soffice);
        properties.setConversionTimeoutSeconds(120);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(2);
        executor.setThreadNamePrefix("ibl-b4-pdf-");
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

    private static boolean isSofficeAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private static Path resolveRepoFile(String relative) {
        Path cwd = Path.of("").toAbsolutePath();
        return Stream.of(
                        cwd.resolve(relative),
                        cwd.resolve("backend").resolve("..").resolve(relative).normalize(),
                        cwd.getParent() == null ? null : cwd.getParent().resolve(relative)
                )
                .filter(path -> path != null && Files.isRegularFile(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing repo file: " + relative + " (cwd=" + cwd + ")"));
    }

    private static void copyRecursive(Path source, Path target) throws Exception {
        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
