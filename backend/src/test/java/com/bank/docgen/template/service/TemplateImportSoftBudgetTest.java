package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * TIP-04 — soft size budgets for TemplateImport peel (AI-SCALE #167).
 * LOC = non-empty lines (aligned with BDD hotspot measurement).
 */
class TemplateImportSoftBudgetTest {

    private static final int SERVICE_SOFT_MAX = 400;
    private static final int SUPPORT_SOFT_MAX = 200;
    private static final int FILE_SOFT_MAX = 500;

    private static final Path SERVICE_DIR = Path.of(
            "src/main/java/com/bank/docgen/template/service"
    );

    @Test
    void templateImportService_underServiceSoftBudget() throws IOException {
        assertNonEmptyLocAtMost(
                SERVICE_DIR.resolve("TemplateImportService.java"),
                SERVICE_SOFT_MAX,
                "TemplateImportService soft Service budget"
        );
    }

    @Test
    void templateImportDependencyPrecheck_underFileSoftBudget() throws IOException {
        assertNonEmptyLocAtMost(
                SERVICE_DIR.resolve("TemplateImportDependencyPrecheck.java"),
                FILE_SOFT_MAX,
                "TemplateImportDependencyPrecheck soft file budget"
        );
    }

    @Test
    void templateImportSupportCollaborators_underSupportSoftBudget() throws IOException {
        try (Stream<Path> stream = Files.list(SERVICE_DIR)) {
            List<Path> supports = stream
                    .filter(path -> path.getFileName().toString().startsWith("TemplateImport"))
                    .filter(path -> path.getFileName().toString().endsWith("Support.java")
                            || path.getFileName().toString().endsWith("Validator.java")
                            || path.getFileName().toString().contains("Precheck")
                                    && !path.getFileName().toString().equals(
                                            "TemplateImportDependencyPrecheck.java"))
                    .sorted()
                    .toList();
            assertThat(supports).isNotEmpty();
            for (Path support : supports) {
                int budget = support.getFileName().toString().endsWith("Support.java")
                        || support.getFileName().toString().endsWith("Validator.java")
                        ? SUPPORT_SOFT_MAX
                        : FILE_SOFT_MAX;
                assertNonEmptyLocAtMost(support, budget, support.getFileName().toString());
            }
        }
    }

    @Test
    void templateImportManuallyMaintainedFiles_underFileSoftBudget() throws IOException {
        try (Stream<Path> stream = Files.list(SERVICE_DIR)) {
            List<Path> importFiles = stream
                    .filter(path -> path.getFileName().toString().startsWith("TemplateImport"))
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
            assertThat(importFiles).isNotEmpty();
            for (Path file : importFiles) {
                assertNonEmptyLocAtMost(file, FILE_SOFT_MAX, file.getFileName().toString());
            }
        }
    }

    private static void assertNonEmptyLocAtMost(Path path, int max, String label) throws IOException {
        assertThat(path).as(label + " must exist").exists();
        long loc = Files.readAllLines(path).stream()
                .filter(line -> !line.isBlank())
                .count();
        assertThat(loc)
                .as("%s non-empty LOC=%d exceeds soft budget %d", label, loc, max)
                .isLessThanOrEqualTo(max);
    }
}
