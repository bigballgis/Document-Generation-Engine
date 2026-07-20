package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoPackageContractSupport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * BDD-DEMO-REFRESH-005 / 011 — Wave A bank-letter content must not contain meta-padding
 * or placeholder scaffolding in deploy demo sources.
 */
class DemoBankLetterContentQualityTest {

    private static final List<String> FORBIDDEN_PHRASES = List.of(
            "for the executive demonstration dataset",
            "will be expanded in the final documentation set",
            "executive test data set",
            "approved test data set",
            "lorem ipsum",
            "sample clause",
            "{{placeholder",
            "placeholder text"
    );

    private static final Pattern FORBIDDEN_TOKEN = Pattern.compile(
            "\\b(LOREM|PLACEHOLDER|TODO)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void bddDemoRefresh011_demoPackageSourcesHaveNoMetaPadding() throws Exception {
        List<String> hits = new ArrayList<>();
        for (String packageCode : DemoPackageContractSupport.packageCodes()) {
            Path root = DemoPackageContractSupport.packageRoot(packageCode);
            scanTree(root.resolve("config"), hits);
            scanTree(root.resolve("sql"), hits);
            Path clauseLibrary = root.resolve("lma-clause-library.ps1");
            if (Files.isRegularFile(clauseLibrary)) {
                scanFile(clauseLibrary, hits);
            }
        }
        Path manifest = DemoPackageContractSupport.deployRoot()
                .resolve("demo-shared/demo-runtime-generate-manifest.json");
        scanFile(manifest, hits);
        Path fullFlowFixture = Path.of("..", "frontend", "e2e", "fixtures", "demo", "full-flow-demo-test-variables.json")
                .normalize();
        if (Files.isRegularFile(fullFlowFixture)) {
            scanFile(fullFlowFixture, hits);
        }

        assertThat(hits)
                .as("Wave A demo sources must not contain meta-padding / placeholder prose")
                .isEmpty();
    }

    @Test
    void bddDemoRefresh005_folSchedulesContainOperativeMarkers() throws Exception {
        Path sql = DemoPackageContractSupport.packageRoot("demo-fol")
                .resolve("sql/001-fol-standard-clauses.sql");
        String body = Files.readString(sql, StandardCharsets.UTF_8);
        assertThat(body).contains("Pacific Rim Holdings");
        assertThat(body).contains("Conditions Precedent");
        assertThat(body).contains("Utilisation Request");
        assertThat(body).contains("Security Principles");
        assertThat(body.toLowerCase(Locale.ROOT)).doesNotContain("executive demonstration dataset");
        assertThat(body.toLowerCase(Locale.ROOT)).doesNotContain("will be expanded in the final documentation set");
    }

    private static void scanTree(Path root, List<String> hits) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".json")
                                || name.endsWith(".sql")
                                || name.endsWith(".ps1")
                                || name.endsWith(".md");
                    })
                    .forEach(path -> {
                        try {
                            scanFile(path, hits);
                        } catch (IOException ex) {
                            throw new IllegalStateException("Failed to scan " + path, ex);
                        }
                    });
        }
    }

    private static void scanFile(Path path, List<String> hits) throws IOException {
        // Manifest documents the forbidden list itself — skip content scan there.
        if (path.getFileName().toString().equals("demo-runtime-generate-manifest.json")) {
            return;
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        String lower = text.toLowerCase(Locale.ROOT);
        for (String phrase : FORBIDDEN_PHRASES) {
            if (lower.contains(phrase)) {
                hits.add(path + " -> phrase: " + phrase);
            }
        }
        if (FORBIDDEN_TOKEN.matcher(text).find()) {
            hits.add(path + " -> token LOREM|PLACEHOLDER|TODO");
        }
    }
}
