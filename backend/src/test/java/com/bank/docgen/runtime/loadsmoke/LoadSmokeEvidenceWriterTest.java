package com.bank.docgen.runtime.loadsmoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadSmokeEvidenceWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesJsonAndMarkdownEvidence() throws Exception {
        LoadSmokeConfig config = LoadSmokeConfig.fromEnvironment();
        // Rebuild with temp evidence dir via system property for isolation.
        String previous = System.getProperty("docgen.loadSmoke.evidenceDir");
        try {
            System.setProperty("docgen.loadSmoke.evidenceDir", tempDir.toString());
            LoadSmokeConfig isolated = LoadSmokeConfig.fromEnvironment();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("scenarioA", Map.of("p95Ms", 1234, "errorRate", 0.0));
            summary.put("scenarioB", Map.of("droppedStreams", 0, "startedStreams", 5));

            Path json = new LoadSmokeEvidenceWriter(new ObjectMapper()).write(isolated, summary);
            assertThat(json).exists();
            assertThat(tempDir.resolve("latest-summary.json")).exists();
            assertThat(tempDir.resolve("latest-summary.md")).exists();
            String md = Files.readString(tempDir.resolve("latest-summary.md"));
            assertThat(md).contains("LR-D6 Load Smoke Evidence").contains("Scenario A");
        } finally {
            if (previous == null) {
                System.clearProperty("docgen.loadSmoke.evidenceDir");
            } else {
                System.setProperty("docgen.loadSmoke.evidenceDir", previous);
            }
        }
        assertThat(config.evidenceDir().toString()).contains("lrp-d6-load-smoke");
    }
}
