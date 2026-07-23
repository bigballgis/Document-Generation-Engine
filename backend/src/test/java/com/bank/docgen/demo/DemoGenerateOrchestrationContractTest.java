package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoPackageContractSupport;
import com.bank.docgen.demo.support.DemoPublishRegistry;
import com.bank.docgen.demo.support.DemoRuntimeGenerateManifest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * BDD-DEMO-TYP-011/012/013 — runtime generate script + manifest contract (P23-T14).
 */
class DemoGenerateOrchestrationContractTest {

    @Test
    void bddDemoTyp013_generateManifestCoversAllPublishRegistryTemplates() throws Exception {
        JsonNode manifest = DemoRuntimeGenerateManifest.load();
        assertThat(manifest.path("manifestVersion").asText()).isEqualTo("keep-bank-letters-v1");
        assertThat(DemoRuntimeGenerateManifest.templateExternalIds(manifest))
                .containsExactlyElementsOf(DemoPublishRegistry.allPublishExternalIds())
                .hasSize(8)
                .doesNotContain("DEMO-FULL-FLOW-LETTER");
    }

    @Test
    void bddDemoTyp013_generateScriptUsesSharedManifestAndRegistry() throws Exception {
        Path script = DemoPackageContractSupport.deployRoot().resolve("generate-all-demos.ps1");
        String content = Files.readString(script);
        assertThat(content).contains("Get-DemoRuntimeGenerateManifest");
        assertThat(content).contains("Get-DemoPublishExternalIds");
        assertThat(content).contains("generated-docx-manifest.json");
        assertThat(content).contains("Resolve-DemoExecutiveVariables");
        assertThat(content).contains("audit-records");
    }

    @Test
    void bddDemoTyp013_sharedHelpersExposeRuntimeGenerateManifestLoader() throws Exception {
        Path shared = DemoPackageContractSupport.deployRoot().resolve("demo-import-shared.ps1");
        String content = Files.readString(shared);
        assertThat(content).contains("function Get-DemoRuntimeGenerateManifest");
        assertThat(content).contains("function Resolve-DemoExecutiveVariables");
    }

    @ParameterizedTest
    @MethodSource("templateEntries")
    void bddDemoTyp012_eachTemplateDefinesSizeFloorMarkersAndFixture(JsonNode entry) throws Exception {
        assertThat(entry.path("externalId").asText()).isNotBlank();
        assertThat(entry.path("minDocxBytes").asInt()).isPositive();
        assertThat(entry.path("contentMarkers").isArray()).isTrue();
        assertThat(entry.path("contentMarkers")).isNotEmpty();
        assertThat(DemoRuntimeGenerateManifest.fixtureExists(entry)).isTrue();
    }

    @Test
    void bddDemoTyp013_manifestDocumentsForbiddenPlaceholderScan() throws Exception {
        JsonNode manifest = DemoRuntimeGenerateManifest.load();
        assertThat(manifest.path("forbiddenPatterns").isArray()).isTrue();
        assertThat(manifest.path("forbiddenPatterns").toString()).contains("LOREM");
        assertThat(manifest.path("forbiddenPatterns").toString()).contains("{{");
        assertThat(manifest.path("forbiddenPatterns").toString())
                .contains("For the executive demonstration dataset");
        assertThat(manifest.path("forbiddenPatterns").toString())
                .contains("will be expanded in the final documentation set");
    }

    private static java.util.stream.Stream<JsonNode> templateEntries() throws Exception {
        JsonNode manifest = DemoRuntimeGenerateManifest.load();
        java.util.List<JsonNode> entries = new java.util.ArrayList<>();
        manifest.path("templates").forEach(entries::add);
        return entries.stream();
    }
}
