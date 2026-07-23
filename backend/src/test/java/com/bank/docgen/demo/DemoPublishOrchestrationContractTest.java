package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoPackageContractSupport;
import com.bank.docgen.demo.support.DemoPublishRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * BDD-DEMO-KEEP-008 / 014 — publish orchestration covers keep-set bank-letter templates only.
 */
class DemoPublishOrchestrationContractTest {

    @Test
    void bddDemoKeep014_publishRegistryCoversKeepSetOnly() throws Exception {
        assertThat(DemoPublishRegistry.allPublishExternalIds())
                .hasSize(8)
                .containsExactly(
                        "CORP-FOL-OFFER",
                        "DEMO-CREDIT-LIMIT-CONFIRM",
                        "DEMO-ANNUAL-REVIEW",
                        "DEMO-FACILITY-RENEWAL",
                        "DEMO-FACILITY-AMENDMENT",
                        "DEMO-COMMITMENT-LETTER",
                        "DEMO-FORMAL-DEMAND",
                        "DEMO-COVENANT-WAIVER"
                );
        assertThat(DemoPublishRegistry.externalIdsFromPackages())
                .hasSize(8)
                .doesNotContain("DEMO-FULL-FLOW-LETTER", "DEMO-RETAIL-LETTER");
    }

    @Test
    void bddDemoTyp011_publishScriptUsesSharedRegistry() throws Exception {
        Path script = DemoPackageContractSupport.deployRoot().resolve("publish-all-demos.ps1");
        String content = Files.readString(script);
        assertThat(content).contains("Get-DemoPublishExternalIds");
        assertThat(content).contains("Get-DemoAllowedApiAdGroups");
        assertThat(content).contains("Ensure-DemoRuntimeCredential");
        assertThat(content).doesNotContain("DEMO-FULL-FLOW-LETTER");
    }

    @Test
    void bddDemoKeep008_importChainListsKeepPackagesOnly() throws Exception {
        Path script = DemoPackageContractSupport.deployRoot().resolve("import-all-demos.ps1");
        String content = Files.readString(script);
        for (String packageCode : DemoPackageContractSupport.packageCodes()) {
            assertThat(content).contains(packageCode);
        }
        assertThat(content).doesNotContain("demo-retail-account");
        assertThat(content).doesNotContain("demo-mortgage");
        assertThat(content).doesNotContain("demo-trade-lc");
        assertThat(content).doesNotContain("demo-collection");
        assertThat(content).doesNotContain("demo-wealth");
        assertThat(content).doesNotContain("demo-kyc-cdd");
        assertThat(content).doesNotContain("demo-account-closure");
        assertThat(content).doesNotContain("demo-insurance-endorsement");
    }

    @ParameterizedTest
    @CsvSource({
            "CORP, CORP_API",
            "RETAIL, RETAIL_API",
            "TRADE, RETAIL_API",
            "WEALTH, RETAIL_API"
    })
    void bddDemoTyp011_adGroupMappingAlignsWithRuntimeCallers(String groupCode, String expectedAdGroup) {
        assertThat(DemoPublishRegistry.allowedApiAdGroups(groupCode)).containsExactly(expectedAdGroup);
    }

    @Test
    void bddDemoTyp011_eachPackageGroupCodeMapsToKnownAdGroup() throws Exception {
        for (String packageCode : DemoPackageContractSupport.packageCodes()) {
            JsonNode config = DemoPackageContractSupport.templateConfig(packageCode);
            String groupCode = config.path("groupCode").asText("");
            assertThat(groupCode).isNotBlank();
            assertThat(DemoPublishRegistry.allowedApiAdGroups(groupCode))
                    .containsAnyOf("RETAIL_API", "CORP_API")
                    .hasSize(1);
        }
    }

    @Test
    void bddDemoKeep007_purgeSeedersAbsentFromClasspath() {
        assertThat(classPresent("com.bank.docgen.demo.DemoFullFlowCatalogSeeder")).isFalse();
        assertThat(classPresent("com.bank.docgen.demo.DemoFullFlowPublishSupport")).isFalse();
        assertThat(classPresent("com.bank.docgen.demo.DemoCatalogSeeder")).isFalse();
        assertThat(classPresent("com.bank.docgen.demo.DemoCatalogSeedProperties")).isFalse();
        assertThat(classPresent("com.bank.docgen.demo.CatalogLoadSeeder")).isTrue();
        assertThat(classPresent("com.bank.docgen.demo.DemoAssetLibrarySeeder")).isTrue();
        // Retained: CatalogLoadSeeder + DemoAssetLibrarySeeder still use letterhead/session helpers.
        assertThat(classPresent("com.bank.docgen.demo.DemoRetailLetterheadDocxBuilder")).isTrue();
        assertThat(classPresent("com.bank.docgen.demo.DemoCatalogSessions")).isTrue();
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
