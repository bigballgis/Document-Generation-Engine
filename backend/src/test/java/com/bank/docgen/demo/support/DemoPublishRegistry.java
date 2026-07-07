package com.bank.docgen.demo.support;

import com.bank.docgen.demo.DemoFullFlowCatalogSeeder;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical demo publish registry for P23-T12 — external IDs and API AD group mapping
 * mirrored by {@code deploy/publish-all-demos.ps1} and {@code deploy/demo-import-shared.ps1}.
 */
public final class DemoPublishRegistry {

    private DemoPublishRegistry() {
    }

    /**
     * Maps template {@code groupCode} to runtime API policy AD groups.
     * {@code CORP} → {@code CORP_API}; all other demo groups → {@code RETAIL_API}
     * (aligned with {@code application.yml} {@code svc-caller} / {@code e2e-runtime-caller}).
     */
    public static List<String> allowedApiAdGroups(String groupCode) {
        return "CORP".equals(groupCode) ? List.of("CORP_API") : List.of("RETAIL_API");
    }

    public static List<String> externalIdsFromPackages() throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        for (String packageCode : DemoPackageContractSupport.packageCodes()) {
            JsonNode config = DemoPackageContractSupport.templateConfig(packageCode);
            collectExternalIds(config, ids);
        }
        return List.copyOf(ids);
    }

    public static List<String> allPublishExternalIds() throws IOException {
        Set<String> ids = new LinkedHashSet<>(externalIdsFromPackages());
        ids.add(DemoFullFlowCatalogSeeder.DEMO_FULL_FLOW_EXTERNAL_ID);
        return orderedPublishExternalIds(ids);
    }

    private static void collectExternalIds(JsonNode config, Set<String> ids) {
        if (config.has("templates") && config.get("templates").isArray()) {
            for (JsonNode template : config.get("templates")) {
                ids.add(template.path("externalId").asText());
            }
        } else if (config.has("templateExternalId")) {
            ids.add(config.path("templateExternalId").asText());
        }
    }

    private static List<String> orderedPublishExternalIds(Set<String> ids) {
        List<String> ordered = new ArrayList<>();
        String[] preferred = {
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
        };
        for (String externalId : preferred) {
            if (ids.contains(externalId)) {
                ordered.add(externalId);
            }
        }
        for (String externalId : ids) {
            if (!ordered.contains(externalId)) {
                ordered.add(externalId);
            }
        }
        return List.copyOf(ordered);
    }
}
