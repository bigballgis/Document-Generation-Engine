package com.bank.docgen.sharedkernel.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * MTF-04 — soft size budgets for mega-test fixture peel (AI-SCALE #169).
 * LOC = non-empty lines (aligned with BDD hotspot measurement).
 */
class MegaTestFixtureSoftBudgetTest {

    private static final int FILE_SOFT_MAX = 500;
    private static final int CRITICAL_HARD_MAX = 800;

    private static final List<Path> PEELED_HOTSPOT_SUITE_FILES = List.of(
            Path.of("src/test/java/com/bank/docgen/template/web/TemplatePlatformLifecycleSliceTest.java"),
            Path.of("src/test/java/com/bank/docgen/template/web/TemplatePlatformRuntimeSliceTest.java"),
            Path.of("src/test/java/com/bank/docgen/template/web/TemplatePlatformDatasetContractSliceTest.java"),
            Path.of("src/test/java/com/bank/docgen/template/web/TemplatePlatformSliceFixtures.java"),
            Path.of(
                    "src/test/java/com/bank/docgen/sharedkernel/document/compute/"
                            + "VariableComputeEngineCoreDslTest.java"
            ),
            Path.of(
                    "src/test/java/com/bank/docgen/sharedkernel/document/compute/"
                            + "VariableComputeEngineFormatLocaleTest.java"
            ),
            Path.of(
                    "src/test/java/com/bank/docgen/sharedkernel/document/compute/"
                            + "VariableComputeEngineSpellAmountSuiteTest.java"
            ),
            Path.of("src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterCoreTest.java"),
            Path.of("src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterModuleMediaTest.java"),
            Path.of("src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterTestFixtures.java"),
            Path.of("src/test/java/com/bank/docgen/template/service/PublishGateServiceCoreTest.java"),
            Path.of("src/test/java/com/bank/docgen/template/service/PublishGateServiceContentModuleTest.java"),
            Path.of("src/test/java/com/bank/docgen/template/service/PublishGateServiceTestFixtures.java"),
            Path.of("src/test/java/com/bank/docgen/library/service/LibraryExportServiceZipArtifactTest.java"),
            Path.of("src/test/java/com/bank/docgen/library/service/LibraryExportServiceAccessFilterTest.java"),
            Path.of("src/test/java/com/bank/docgen/library/service/LibraryExportServiceTestFixtures.java"),
            Path.of("src/test/java/com/bank/docgen/template/service/SysNormPromotionPackTest.java"),
            Path.of("src/test/java/com/bank/docgen/template/service/SysNormPromotionPackTestFixtures.java")
    );

    private static final List<Path> REMOVED_MEGA_SUITE_FILES = List.of(
            Path.of("src/test/java/com/bank/docgen/template/web/TemplatePlatformSliceTest.java"),
            Path.of(
                    "src/test/java/com/bank/docgen/sharedkernel/document/compute/"
                            + "VariableComputeEngineTest.java"
            ),
            Path.of("src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterTest.java"),
            Path.of("src/test/java/com/bank/docgen/template/service/PublishGateServiceTest.java"),
            Path.of("src/test/java/com/bank/docgen/library/service/LibraryExportServiceTest.java")
    );

    @Test
    void peeledHotspotSuites_underSoftFileBudget() throws IOException {
        for (Path path : PEELED_HOTSPOT_SUITE_FILES) {
            assertNonEmptyLocAtMost(path, FILE_SOFT_MAX, path.getFileName().toString());
        }
    }

    @Test
    void peeledHotspotSuites_underCriticalHardBand() throws IOException {
        for (Path path : PEELED_HOTSPOT_SUITE_FILES) {
            assertNonEmptyLocAtMost(path, CRITICAL_HARD_MAX, path.getFileName().toString() + " hard-band");
        }
    }

    @Test
    void formerMegaSuites_areRemoved() {
        for (Path path : REMOVED_MEGA_SUITE_FILES) {
            assertThat(path)
                    .as("%s should be peeled away", path.getFileName())
                    .doesNotExist();
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
