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
 * BDD-DEMO-TYP-011 — publish orchestration covers all demo templates with AD group alignment (P23-T12).
 */
class DemoPublishOrchestrationContractTest {

    @Test
    void bddDemoTyp011_publishRegistryCoversAllPackageTemplates() throws Exception {
        assertThat(DemoPublishRegistry.allPublishExternalIds())
                .hasSize(13)
                .containsExactly(
                        "CORP-FOL-OFFER",
                        DemoFullFlowCatalogSeeder.DEMO_FULL_FLOW_EXTERNAL_ID,
                        "DEMO-RETAIL-ACCOUNT-OPEN",
                        "DEMO-RETAIL-ACCOUNT-BALANCE",
                        "DEMO-MORTGAGE-APPROVAL",
                        "DEMO-CREDIT-LIMIT-CONFIRM",
                        "DEMO-TRADE-LC-NOTICE",
                        "DEMO-TRADE-GUARANTEE-NOTICE",
                        "DEMO-RATE-CHANGE-NOTICE",
                        "DEMO-OVERDUE-COLLECTION",
                        "DEMO-ANNUAL-REVIEW",
                        "DEMO-FACILITY-RENEWAL",
                        "DEMO-WEALTH-STATEMENT"
                );
        assertThat(DemoPublishRegistry.externalIdsFromPackages())
                .hasSize(12)
                .doesNotContain(DemoFullFlowCatalogSeeder.DEMO_FULL_FLOW_EXTERNAL_ID);
    }

    @Test
    void bddDemoTyp011_publishScriptUsesSharedRegistry() throws Exception {
        Path script = DemoPackageContractSupport.deployRoot().resolve("publish-all-demos.ps1");
        String content = Files.readString(script);
        assertThat(content).contains("Get-DemoPublishExternalIds");
        assertThat(content).contains("Get-DemoAllowedApiAdGroups");
        assertThat(content).contains("Ensure-DemoRuntimeCredential");
    }

    @Test
    void bddDemoTyp011_importChainListsAllEightPackages() throws Exception {
        Path script = DemoPackageContractSupport.deployRoot().resolve("import-all-demos.ps1");
        String content = Files.readString(script);
        for (String packageCode : DemoPackageContractSupport.packageCodes()) {
            assertThat(content).contains(packageCode);
        }
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
}
