package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoPackageContractSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * BDD-DEMO-EXP-012 — import idempotency contract: catalogMarker present and stable per package.
 */
class DemoImportIdempotencyContractTest {

    @Test
    void bddDemoExp012_allPackagesDeclareUniqueCatalogMarkers() throws Exception {
        java.util.Set<String> markers = new java.util.HashSet<>();
        for (String packageCode : DemoPackageContractSupport.packageCodes()) {
            JsonNode config = DemoPackageContractSupport.templateConfig(packageCode);
            String marker = config.path("catalogMarker").asText("");
            assertThat(marker).isNotBlank();
            assertThat(markers.add(marker))
                    .as("Duplicate catalogMarker: " + marker)
                    .isTrue();

            Path manifestPath = findManifest(packageCode);
            JsonNode manifest = DemoPackageContractSupport.readJson(manifestPath);
            assertThat(manifest.path("catalogMarker").asText()).isEqualTo(marker);
        }
    }

    @Test
    void bddDemoExp012_importScriptsReferenceSharedHelperOrFullImport() throws Exception {
        for (String packageCode : DemoPackageContractSupport.packageCodes()) {
            if ("demo-fol".equals(packageCode)) {
                continue;
            }
            String shortCode = packageCode.replace("demo-", "");
            Path importScript = DemoPackageContractSupport.packageRoot(packageCode)
                    .resolve("import-" + shortCode + "-demo.ps1");
            String content = Files.readString(importScript);
            assertThat(content).contains("demo-import-shared.ps1");
            assertThat(content).contains("Import-DemoPackage");
        }
    }

    private static Path findManifest(String packageCode) throws Exception {
        Path configDir = DemoPackageContractSupport.packageRoot(packageCode).resolve("config");
        return Files.list(configDir)
                .filter(path -> path.getFileName().toString().endsWith("-catalog-manifest.json"))
                .findFirst()
                .orElseThrow();
    }
}
