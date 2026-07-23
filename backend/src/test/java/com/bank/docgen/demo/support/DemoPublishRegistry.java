package com.bank.docgen.demo.support;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical demo publish registry for keep-set bank letters (TM #164) — external IDs
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
        return orderedPublishExternalIds(new LinkedHashSet<>(externalIdsFromPackages()));
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
                "DEMO-CREDIT-LIMIT-CONFIRM",
                "DEMO-ANNUAL-REVIEW",
                "DEMO-FACILITY-RENEWAL",
                "DEMO-FACILITY-AMENDMENT",
                "DEMO-COMMITMENT-LETTER",
                "DEMO-FORMAL-DEMAND",
                "DEMO-COVENANT-WAIVER"
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
