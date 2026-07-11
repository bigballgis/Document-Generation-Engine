package com.bank.docgen.runtime.loadsmoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes machine-readable + Markdown evidence under {@code docs/plan/evidence/lrp-d6-load-smoke/}.
 */
public final class LoadSmokeEvidenceWriter {

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final ObjectMapper mapper;

    public LoadSmokeEvidenceWriter(ObjectMapper mapper) {
        this.mapper = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path write(LoadSmokeConfig config, Map<String, Object> summary) throws IOException {
        Files.createDirectories(config.evidenceDir());
        Instant now = Instant.now();
        String stamp = FILE_TS.format(now);
        Path jsonPath = config.evidenceDir().resolve("results-" + stamp + ".json");
        Path mdPath = config.evidenceDir().resolve("results-" + stamp + ".md");
        Path latestJson = config.evidenceDir().resolve("latest-summary.json");
        Path latestMd = config.evidenceDir().resolve("latest-summary.md");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskIds", java.util.List.of("lrp-d6-load-smoke", "TaskMaster#37", "LR-D6"));
        payload.put("measuredAt", now.toString());
        payload.put("datePlaceholder", "<date>");
        payload.put("stackVersion", config.stackVersion());
        payload.put("hardwareNote", config.hardwareNote());
        payload.put("baseUrl", config.baseUrl());
        payload.put("templateExternalId", config.templateExternalId());
        payload.put("syncConcurrency", config.syncConcurrency());
        payload.put("sseConcurrency", config.sseConcurrency());
        payload.putAll(summary);

        String json = mapper.writeValueAsString(payload);
        Files.writeString(jsonPath, json, StandardCharsets.UTF_8);
        Files.writeString(latestJson, json, StandardCharsets.UTF_8);

        String md = toMarkdown(payload);
        Files.writeString(mdPath, md, StandardCharsets.UTF_8);
        Files.writeString(latestMd, md, StandardCharsets.UTF_8);
        return jsonPath;
    }

    static String toMarkdown(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("# LR-D6 Load Smoke Evidence\n\n");
        sb.append("**measuredAt:** ").append(payload.get("measuredAt")).append('\n');
        sb.append("**stackVersion:** ").append(payload.get("stackVersion")).append('\n');
        sb.append("**hardwareNote:** ").append(payload.get("hardwareNote")).append('\n');
        sb.append("**baseUrl:** ").append(payload.get("baseUrl")).append('\n');
        sb.append("**templateExternalId:** ").append(payload.get("templateExternalId")).append('\n');
        sb.append('\n');
        Object scenarioA = payload.get("scenarioA");
        Object scenarioB = payload.get("scenarioB");
        sb.append("## Scenario A — Concurrent sync generation\n\n");
        sb.append("```\n").append(String.valueOf(scenarioA)).append("\n```\n\n");
        sb.append("## Scenario B — Parallel SSE preview streams\n\n");
        sb.append("```\n").append(String.valueOf(scenarioB)).append("\n```\n\n");
        sb.append("## Notes\n\n");
        sb.append("- Do not tune thresholds to pass; record observed reality.\n");
        sb.append("- Pool rejections surface as `PDF_CONVERSION_CAPACITY_EXCEEDED`.\n");
        sb.append("- SSE terminal events: `completed` | `failed`.\n");
        return sb.toString();
    }
}
