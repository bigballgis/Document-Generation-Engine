package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoPackageContractSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * BDD-DEMO-EXP-011 — demo package layout mirrors demo-fol structure contract.
 */
class DemoPackageStructureContractTest {

    static Iterable<String> packageCodes() {
        return DemoPackageContractSupport.packageCodes();
    }

    @ParameterizedTest
    @MethodSource("packageCodes")
    void bddDemoExp011_packageMatchesStructureContract(String packageCode) throws Exception {
        DemoPackageContractSupport.assertPackageStructure(packageCode);

        JsonNode config = DemoPackageContractSupport.templateConfig(packageCode);
        assertThat(config.path("catalogMarker").asText()).isNotBlank();

        Path manifestPath = findManifest(packageCode);
        JsonNode manifest = DemoPackageContractSupport.readJson(manifestPath);
        assertThat(manifest.path("catalogMarker").asText()).isEqualTo(config.path("catalogMarker").asText());
    }

    private static Path findManifest(String packageCode) throws Exception {
        Path configDir = DemoPackageContractSupport.packageRoot(packageCode).resolve("config");
        return Files.list(configDir)
                .filter(path -> path.getFileName().toString().endsWith("-catalog-manifest.json"))
                .findFirst()
                .orElseThrow();
    }
}
