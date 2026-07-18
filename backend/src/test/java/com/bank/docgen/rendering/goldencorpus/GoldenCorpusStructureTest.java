package com.bank.docgen.rendering.goldencorpus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CE-K07 structure gate: BDD-CE-K07-001…004, 011–017.
 */
class GoldenCorpusStructureTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GoldenCorpusScanner scanner = new GoldenCorpusScanner(objectMapper);

    @Test
    void goldenCorpusRootDirectoryExists() {
        Path root = scanner.resolveRootDirectory();
        assertThat(root).exists().isDirectory();
        assertThat(root.getFileName().toString()).isEqualTo(GoldenCorpusThemes.ROOT_RESOURCE);
    }

    @Test
    void discoversAtLeastEightRequiredThemePackages() {
        List<GoldenCorpusPackage> packages = scanner.scanAndValidate();

        assertThat(packages).hasSizeGreaterThanOrEqualTo(8);
        Set<String> ids = packages.stream().map(GoldenCorpusPackage::id).collect(Collectors.toSet());
        assertThat(ids).containsAll(GoldenCorpusThemes.REQUIRED_THEME_IDS);
    }

    @Test
    void everyPackageHasUnifiedLayout() {
        List<GoldenCorpusPackage> packages = scanner.scanAndValidate();
        for (GoldenCorpusPackage corpusPackage : packages) {
            Path dir = corpusPackage.directory();
            assertThat(dir.resolve("manifest.json")).isRegularFile();
            assertThat(dir.resolve("input/master.docx")).isRegularFile();
            assertThat(dir.resolve("input/template.json")).isRegularFile();
            assertThat(dir.resolve("input/variables.json")).isRegularFile();
            assertThat(dir.resolve("expected/docx-assertions.json")).isRegularFile();
            assertThat(dir.resolve("expected/pdf-assertions.json")).isRegularFile();
            assertThat(corpusPackage.manifest().id()).isNotBlank();
            assertThat(corpusPackage.manifest().theme()).isNotBlank();
            assertThat(corpusPackage.maturity()).isIn(
                    GoldenCorpusMaturity.ACTIVE,
                    GoldenCorpusMaturity.PLACEHOLDER
            );
        }
    }

    @Test
    void placeholderMissingSkeletonFileFails(@TempDir Path tempDir) throws Exception {
        // Use a remaining PLACEHOLDER theme if any; otherwise force maturity for layout gate.
        Path packageDir = copyPackageToTemp("long-clause-limits", tempDir);
        Path manifestPath = packageDir.resolve("manifest.json");
        String manifest = Files.readString(manifestPath).replace("\"ACTIVE\"", "\"PLACEHOLDER\"");
        Files.writeString(manifestPath, manifest);
        Files.delete(packageDir.resolve("input/template.json"));

        GoldenCorpusPackage loaded = scanner.loadPackage(packageDir);
        assertThat(loaded.maturity()).isEqualTo(GoldenCorpusMaturity.PLACEHOLDER);
        assertThatThrownBy(() -> scanner.validatePackageLayout(loaded))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("template.json");
    }

    @Test
    void placeholderThemesAreEnumeratedWithoutBusinessAssertions() {
        List<GoldenCorpusPackage> packages = scanner.scanAndValidate();
        Set<String> placeholders = packages.stream()
                .filter(pkg -> pkg.maturity() == GoldenCorpusMaturity.PLACEHOLDER)
                .map(GoldenCorpusPackage::id)
                .collect(Collectors.toSet());
        Set<String> active = packages.stream()
                .filter(pkg -> pkg.maturity() == GoldenCorpusMaturity.ACTIVE)
                .map(GoldenCorpusPackage::id)
                .collect(Collectors.toSet());

        assertThat(active).contains(
                "dual-font-master",
                "nested-clauses",
                "encrypted-pdf",
                "specimen-watermark",
                "compute-variables",
                "chinese-uppercase-amount",
                "cross-page-table",
                "qr-barcode",
                "attachment-list",
                "pdfa-2b",
                "long-clause-limits"
        );
        assertThat(placeholders).doesNotContain(
                "long-clause-limits",
                "cross-page-table",
                "specimen-watermark",
                "dual-font-master",
                "compute-variables",
                "chinese-uppercase-amount",
                "attachment-list"
        );
    }

    @Test
    void missingRequiredThemeFailsClosed(@TempDir Path tempDir) throws Exception {
        Path root = scanner.resolveRootDirectory();
        Path mirror = tempDir.resolve("golden-corpus");
        Files.createDirectories(mirror);
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .filter(dir -> Files.isRegularFile(dir.resolve("manifest.json")))
                    .filter(dir -> {
                        try {
                            String id = objectMapper.readTree(Files.readString(dir.resolve("manifest.json")))
                                    .path("id")
                                    .asText();
                            return !"specimen-watermark".equals(id);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .forEach(dir -> {
                        try {
                            copyRecursive(dir, mirror.resolve(dir.getFileName().toString()));
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }

        GoldenCorpusScanner isolated = new GoldenCorpusScanner(objectMapper, mirror);

        assertThatThrownBy(isolated::scanAndValidate)
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("specimen-watermark");
    }

    private Path copyPackageToTemp(String themeId, Path tempDir) throws Exception {
        GoldenCorpusPackage match = scanner.scanAndValidate().stream()
                .filter(pkg -> themeId.equals(pkg.id()))
                .findFirst()
                .orElseThrow();
        Path target = tempDir.resolve(match.directory().getFileName().toString());
        copyRecursive(match.directory(), target);
        return target;
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
