package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoPackageContractSupport;
import com.bank.docgen.demo.support.FolDemoContentModuleSupport;
import com.bank.docgen.rendering.DocxAssembler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

/**
 * BDD-DEMO-EXP-010 — FOL executive DOCX assembly via {@link DocxAssembler#assembleStructuredFromBytes}.
 */
class FolExecutiveDocxAssemblyTest {

    private static final Path FOL_ROOT = DemoPackageContractSupport.packageRoot("demo-fol");
    private static final int MIN_BODY_CHARACTERS_FOR_EXECUTIVE_SCALE = 180_000;
    private static final int MIN_PARAGRAPHS_FOR_EXECUTIVE_SCALE = 600;

    private final DocxAssembler assembler = new DocxAssembler(new ObjectMapper());

    @Test
    void bddDemoExp010_executiveDatasetAssemblesLargeStructuredDocx() throws Exception {
        JsonNode manifest = DemoPackageContractSupport.readJson(FOL_ROOT.resolve("config/fol-catalog-manifest.json"));
        Map<String, String> bindingJsonByAnchor = FolDemoContentModuleSupport.loadBindingJsonByAnchor(
                FOL_ROOT.resolve("config/fol-binding-overlays.json")
        );
        Map<String, String> pinnedModules = FolDemoContentModuleSupport.loadPinnedModulesFromSql(
                FOL_ROOT.resolve("sql/001-fol-standard-clauses.sql"),
                manifest
        );
        Map<String, Object> variables = FolDemoContentModuleSupport.loadExecutiveVariables(
                FOL_ROOT.resolve("config/fol-demo-test-variables.json")
        );

        byte[] master = FolMasterDocxAssetGeneratorTest.buildWholesaleFolMasterDocx();
        byte[] assembled = assembler.assembleStructuredFromBytes(
                master,
                bindingJsonByAnchor,
                variables,
                pinnedModules
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(assembled))) {
            int paragraphCount = document.getParagraphs().size();
            int tableCount = document.getTables().size();
            int totalChars = document.getParagraphs().stream()
                    .mapToInt(paragraph -> paragraph.getText() == null ? 0 : paragraph.getText().length())
                    .sum();
            for (XWPFTable table : document.getTables()) {
                totalChars += table.getText().length();
            }

            assertThat(paragraphCount).isGreaterThanOrEqualTo(MIN_PARAGRAPHS_FOR_EXECUTIVE_SCALE);
            assertThat(totalChars).isGreaterThanOrEqualTo(MIN_BODY_CHARACTERS_FOR_EXECUTIVE_SCALE);
            assertThat(tableCount).isGreaterThanOrEqualTo(3);
            String bodyText = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText() == null ? "" : paragraph.getText())
                    .reduce("", String::concat);
            assertThat(bodyText).contains("Pacific Rim");
        }
    }
}
