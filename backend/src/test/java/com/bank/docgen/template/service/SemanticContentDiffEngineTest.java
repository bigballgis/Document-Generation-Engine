package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.template.api.ChangeDiffHumanReadableEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemanticContentDiffEngineTest {

    private SemanticContentDiffEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SemanticContentDiffEngine(new ObjectMapper());
    }

    @Test
    void detectsSentenceLevelTextModification() {
        Map<String, String> baseline = Map.of("BODY", paragraphTree("贷款利率 4.9%"));
        Map<String, String> candidate = Map.of("BODY", paragraphTree("贷款利率 5.2%"));

        List<ChangeDiffHumanReadableEntry> entries = engine.diffAnchors(baseline, candidate).entries();

        assertThat(entries).isNotEmpty();
        ChangeDiffHumanReadableEntry modified = entries.stream()
                .filter(entry -> "MODIFIED".equals(entry.changeType()))
                .findFirst()
                .orElseThrow();
        assertThat(modified.summary()).contains("贷款利率 4.9%");
        assertThat(modified.summary()).contains("贷款利率 5.2%");
        assertThat(modified.summary()).doesNotContain("masterCatalogVersion");
    }

    @Test
    void detectsAddedRemovedMovedAndNestedChanges() {
        Map<String, String> baseline = Map.of(
                "BODY",
                """
                {"nodes":[
                  {"type":"paragraph","children":[{"type":"textRun","value":"Keep"}]},
                  {"type":"paragraph","children":[{"type":"textRun","value":"MoveMe"}]},
                  {"type":"paragraph","children":[{"type":"textRun","value":"DeleteMe"}]},
                  {"type":"conditionBlock","conditionExpression":"${x}","children":[
                    {"type":"paragraph","children":[{"type":"textRun","value":"Nested old"}]}
                  ]}
                ]}
                """
        );
        Map<String, String> candidate = Map.of(
                "BODY",
                """
                {"nodes":[
                  {"type":"paragraph","children":[{"type":"textRun","value":"Keep"}]},
                  {"type":"paragraph","children":[{"type":"textRun","value":"Added"}]},
                  {"type":"paragraph","children":[{"type":"textRun","value":"MoveMe"}]},
                  {"type":"conditionBlock","conditionExpression":"${x}","children":[
                    {"type":"paragraph","children":[{"type":"textRun","value":"Nested new"}]}
                  ]}
                ]}
                """
        );

        SemanticContentDiffEngine.Result result = engine.diffAnchors(baseline, candidate);

        assertThat(result.entries().stream().map(ChangeDiffHumanReadableEntry::changeType).toList())
                .contains("ADDED", "REMOVED", "MOVED", "MODIFIED");
        assertThat(result.entries().stream().anyMatch(entry ->
                "MODIFIED".equals(entry.changeType()) && entry.summary().contains("Nested old")))
                .isTrue();
    }

    @Test
    void truncatesLongTextSnippets() {
        String longOld = "A".repeat(200);
        String longNew = "B".repeat(200);
        Map<String, String> baseline = Map.of("BODY", paragraphTree(longOld));
        Map<String, String> candidate = Map.of("BODY", paragraphTree(longNew));

        ChangeDiffHumanReadableEntry entry = engine.diffAnchors(baseline, candidate).entries().getFirst();

        assertThat(entry.summary()).contains("…");
        assertThat(entry.summary().length()).isLessThan(longOld.length() + longNew.length());
    }

    @Test
    void returnsEmptyWhenTreesMatch() {
        Map<String, String> same = Map.of("BODY", paragraphTree("same"));

        assertThat(engine.diffAnchors(same, new LinkedHashMap<>(same)).entries()).isEmpty();
    }

    private static String paragraphTree(String text) {
        return "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"textRun\",\"value\":\""
                + text + "\"}]}]}";
    }
}
