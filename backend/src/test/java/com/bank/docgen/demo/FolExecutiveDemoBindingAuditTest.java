package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoPackageContractSupport;
import com.bank.docgen.demo.support.FolDemoContentModuleSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * BDD-DEMO-EXP-010 — FOL executive scale: 40 anchors bound with rich structured content.
 */
class FolExecutiveDemoBindingAuditTest {

    private static final Path FOL_ROOT = DemoPackageContractSupport.packageRoot("demo-fol");
    private static final int EXPECTED_ANCHOR_COUNT = 40;

    @Test
    void bddDemoExp010_allFortyAnchorsBoundInOverlays() throws Exception {
        JsonNode manifest = DemoPackageContractSupport.readJson(FOL_ROOT.resolve("config/fol-catalog-manifest.json"));
        JsonNode masterAnchorIds = manifest.path("masterAnchorIds");
        assertThat(masterAnchorIds).hasSize(EXPECTED_ANCHOR_COUNT);

        JsonNode overlays = DemoPackageContractSupport.readJson(FOL_ROOT.resolve("config/fol-binding-overlays.json"));
        JsonNode bindings = overlays.path("bindings");
        Set<String> missing = new HashSet<>();
        masterAnchorIds.forEach(anchorId -> {
            if (!bindings.has(anchorId.asText())) {
                missing.add(anchorId.asText());
            }
        });
        assertThat(missing).isEmpty();
    }

    @Test
    void bddDemoExp010_manifestDeclaresFolPageTarget() throws Exception {
        JsonNode manifest = DemoPackageContractSupport.readJson(FOL_ROOT.resolve("config/fol-catalog-manifest.json"));
        assertThat(manifest.path("folPageTarget").asInt()).isGreaterThanOrEqualTo(100);
    }

    @Test
    void bddDemoExp010_richStructuredNodeTypesPresentAcrossBindings() throws Exception {
        String overlaysJson = Files.readString(FOL_ROOT.resolve("config/fol-binding-overlays.json"));
        assertThat(overlaysJson).contains("\"emphasis\"");
        assertThat(overlaysJson).contains("\"styleRef\"");
        assertThat(overlaysJson).contains("\"type\":  \"list\"");
        assertThat(overlaysJson).contains("\"ordered\":  true");
        assertThat(overlaysJson).contains("\"contentModuleRef\"");
        assertThat(overlaysJson).contains("\"conditionBlock\"");
        assertThat(overlaysJson).contains("\"loopBlock\"");
        assertThat(overlaysJson).contains("\"tableComponentRef\"");
    }

    @Test
    void bddDemoExp010_sqlContentModulesLoadForClauseBindings() throws Exception {
        JsonNode manifest = DemoPackageContractSupport.readJson(FOL_ROOT.resolve("config/fol-catalog-manifest.json"));
        var pinned = FolDemoContentModuleSupport.loadPinnedModulesFromSql(
                FOL_ROOT.resolve("sql/001-fol-standard-clauses.sql"),
                manifest
        );
        assertThat(pinned).hasSizeGreaterThanOrEqualTo(30);
        manifest.path("clauseBindings").forEach(binding -> {
            String anchorId = binding.path("anchorId").asText();
            if (pinned.containsKey(anchorId)) {
                assertThat(pinned.get(anchorId)).contains("\"blocks\"");
            }
        });
    }
}
