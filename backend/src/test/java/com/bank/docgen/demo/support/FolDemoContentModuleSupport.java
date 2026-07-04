package com.bank.docgen.demo.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FolDemoContentModuleSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern MODULE_COMMENT = Pattern.compile(
            "--\\s+(MOD-FOL-[A-Z0-9-]+)\\s+->\\s+(FOL_[A-Z0-9_]+)"
    );
    private static final Pattern CONTENT_JSON = Pattern.compile(
            "content_structure_json\\s*=\\s*'(\\{\"blocks\":.*?\\})'",
            Pattern.DOTALL
    );

    private FolDemoContentModuleSupport() {
    }

    public static Map<String, String> loadPinnedModulesFromSql(Path sqlPath, JsonNode catalogManifest) throws IOException {
        String sql = Files.readString(sqlPath);
        Map<String, String> moduleCodeToAnchor = new LinkedHashMap<>();
        catalogManifest.path("clauseBindings").forEach(binding -> {
            String anchorId = binding.path("anchorId").asText("");
            String moduleCode = binding.path("moduleCode").asText("");
            if (!anchorId.isBlank() && !moduleCode.isBlank()) {
                moduleCodeToAnchor.put(moduleCode, anchorId);
            }
        });

        Map<String, String> pinned = new LinkedHashMap<>();
        Matcher commentMatcher = MODULE_COMMENT.matcher(sql);
        while (commentMatcher.find()) {
            String moduleCode = commentMatcher.group(1);
            String anchorFromComment = commentMatcher.group(2);
            String anchorId = moduleCodeToAnchor.getOrDefault(moduleCode, anchorFromComment);
            int start = commentMatcher.end();
            int end = Math.min(sql.length(), start + 500_000);
            String segment = sql.substring(start, end);
            Matcher jsonMatcher = CONTENT_JSON.matcher(segment);
            if (jsonMatcher.find()) {
                String json = jsonMatcher.group(1).replace("''", "'");
                pinned.putIfAbsent(anchorId, json);
            }
        }
        return pinned;
    }

    public static Map<String, String> loadBindingJsonByAnchor(Path overlaysPath) throws IOException {
        String raw = Files.readString(overlaysPath);
        if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
            raw = raw.substring(1);
        }
        JsonNode root = OBJECT_MAPPER.readTree(raw);
        Map<String, String> bindings = new LinkedHashMap<>();
        JsonNode bindingsNode = root.path("bindings");
        bindingsNode.fieldNames().forEachRemaining(anchorId -> {
            try {
                bindings.put(anchorId, OBJECT_MAPPER.writeValueAsString(bindingsNode.get(anchorId)));
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to serialize binding for " + anchorId, ex);
            }
        });
        return bindings;
    }

    public static Map<String, Object> loadExecutiveVariables(Path testVariablesPath) throws IOException {
        String raw = Files.readString(testVariablesPath);
        if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
            raw = raw.substring(1);
        }
        JsonNode root = OBJECT_MAPPER.readTree(raw);
        JsonNode variables = root.path("variables");
        if (variables.isMissingNode() || variables.isNull()) {
            return Map.of();
        }
        return OBJECT_MAPPER.convertValue(variables, Map.class);
    }
}
