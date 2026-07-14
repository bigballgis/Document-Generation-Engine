package com.bank.docgen.rendering.goldencorpus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * CE-K07 harness + ACTIVE samples: BDD-CE-K07-005…010, 019.
 */
class GoldenCorpusHarnessTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GoldenCorpusScanner scanner = new GoldenCorpusScanner(OBJECT_MAPPER);
    private final GoldenCorpusActiveRunner runner = new GoldenCorpusActiveRunner(OBJECT_MAPPER);
    private final GoldenCorpusAssertionLoader assertionLoader = new GoldenCorpusAssertionLoader(OBJECT_MAPPER);

    static Stream<String> activePackageIds() {
        GoldenCorpusScanner localScanner = new GoldenCorpusScanner(OBJECT_MAPPER);
        return localScanner.scanAndValidate().stream()
                .filter(pkg -> pkg.maturity() == GoldenCorpusMaturity.ACTIVE)
                .map(GoldenCorpusPackage::id);
    }

    @Test
    void activeAndPlaceholderAreSplitCorrectly() {
        List<GoldenCorpusPackage> packages = scanner.scanAndValidate();
        Set<String> active = packages.stream()
                .filter(pkg -> pkg.maturity() == GoldenCorpusMaturity.ACTIVE)
                .map(GoldenCorpusPackage::id)
                .collect(Collectors.toSet());
        Set<String> placeholders = packages.stream()
                .filter(pkg -> pkg.maturity() == GoldenCorpusMaturity.PLACEHOLDER)
                .map(GoldenCorpusPackage::id)
                .collect(Collectors.toSet());

        assertThat(active).contains("nested-clauses", "encrypted-pdf", "specimen-watermark", "dual-font-master");
        assertThat(placeholders).doesNotContain("nested-clauses", "encrypted-pdf", "specimen-watermark", "dual-font-master");
        assertThat(active.size()).isGreaterThanOrEqualTo(3);
    }

    @ParameterizedTest(name = "ACTIVE {0}")
    @MethodSource("activePackageIds")
    void runsActivePackage(String packageId) throws Exception {
        GoldenCorpusPackage corpusPackage = scanner.scanAndValidate().stream()
                .filter(pkg -> packageId.equals(pkg.id()))
                .findFirst()
                .orElseThrow();
        runner.runOne(corpusPackage);
    }

    @Test
    void rejectsPixelAssertionConfiguration(@TempDir Path tempDir) throws Exception {
        Path assertionFile = tempDir.resolve("docx-assertions.json");
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode assertions = root.putArray("assertions");
        ObjectNode bad = assertions.addObject();
        bad.put("type", "PIXEL_COMPARE");
        bad.put("image", "expected.png");
        Files.writeString(assertionFile, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));

        assertThatThrownBy(() -> assertionLoader.loadAndValidate(
                assertionFile,
                Set.of("XML_CONTAINS", "XPATH_EXISTS")
        ))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("PIXEL");
    }

    @Test
    void activeDocxRegressionFailureIsFailClosed(@TempDir Path tempDir) throws Exception {
        GoldenCorpusPackage nested = scanner.scanAndValidate().stream()
                .filter(pkg -> "nested-clauses".equals(pkg.id()))
                .findFirst()
                .orElseThrow();

        Path copy = tempDir.resolve(nested.directory().getFileName().toString());
        copyRecursive(nested.directory(), copy);

        Path docxAssertions = copy.resolve("expected/docx-assertions.json");
        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(docxAssertions));
        ((ObjectNode) root.path("assertions").get(0))
                .put("substring", "THIS_SUBSTRING_MUST_NOT_EXIST_IN_DOCX_XML");
        Files.writeString(docxAssertions, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));

        GoldenCorpusPackage broken = scanner.loadPackage(copy);
        assertThatThrownBy(() -> runner.runOne(broken))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("DOCX assertion failed");
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
