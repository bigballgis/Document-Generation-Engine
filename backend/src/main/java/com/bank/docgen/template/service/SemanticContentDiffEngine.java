package com.bank.docgen.template.service;

import com.bank.docgen.authoring.structured.StructuredContentNodeType;
import com.bank.docgen.template.api.ChangeDiffHumanReadableEntry;
import com.bank.docgen.template.api.ChangeDiffModificationView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Semantic structured-content tree diff (anchor → block path) using sibling LCS alignment.
 */
final class SemanticContentDiffEngine {

    static final int TEXT_SNIPPET_MAX = 120;

    private final ObjectMapper objectMapper;

    SemanticContentDiffEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Result diffAnchors(Map<String, String> baselineByAnchor, Map<String, String> candidateByAnchor) {
        List<ChangeDiffHumanReadableEntry> entries = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<ChangeDiffModificationView> modified = new ArrayList<>();

        Set<String> anchorIds = new TreeSet<>();
        anchorIds.addAll(baselineByAnchor.keySet());
        anchorIds.addAll(candidateByAnchor.keySet());

        for (String anchorId : anchorIds) {
            String baselineJson = baselineByAnchor.get(anchorId);
            String candidateJson = candidateByAnchor.get(anchorId);
            if (baselineJson == null && candidateJson != null) {
                String summary = "Anchor '" + anchorId + "' content added";
                added.add(anchorId);
                entries.add(new ChangeDiffHumanReadableEntry("ADDED", anchorId, summary));
                continue;
            }
            if (baselineJson != null && candidateJson == null) {
                String summary = "Anchor '" + anchorId + "' content removed";
                removed.add(anchorId);
                entries.add(new ChangeDiffHumanReadableEntry("REMOVED", anchorId, summary));
                continue;
            }
            if (Objects.equals(baselineJson, candidateJson)) {
                continue;
            }
            List<BlockNode> baselineBlocks = toBlocks(parseNodes(baselineJson));
            List<BlockNode> candidateBlocks = toBlocks(parseNodes(candidateJson));
            diffBlockLists(anchorId, baselineBlocks, candidateBlocks, entries, added, removed, modified);
        }
        return new Result(List.copyOf(added), List.copyOf(removed), List.copyOf(modified), List.copyOf(entries));
    }

    private void diffBlockLists(
            String parentPath,
            List<BlockNode> baseline,
            List<BlockNode> candidate,
            List<ChangeDiffHumanReadableEntry> entries,
            List<String> added,
            List<String> removed,
            List<ChangeDiffModificationView> modified
    ) {
        boolean[] baselineMatched = new boolean[baseline.size()];
        boolean[] candidateMatched = new boolean[candidate.size()];

        matchAndEmit(
                parentPath,
                baseline,
                candidate,
                baselineMatched,
                candidateMatched,
                true,
                entries,
                added,
                removed,
                modified
        );
        matchAndEmit(
                parentPath,
                baseline,
                candidate,
                baselineMatched,
                candidateMatched,
                false,
                entries,
                added,
                removed,
                modified
        );

        for (int index = 0; index < baseline.size(); index++) {
            if (baselineMatched[index]) {
                continue;
            }
            BlockNode block = baseline.get(index);
            String path = parentPath + "/nodes[" + index + "]";
            String summary = path + ": removed '" + truncate(block.text()) + "'";
            removed.add(path);
            entries.add(new ChangeDiffHumanReadableEntry("REMOVED", path, summary));
        }
        for (int index = 0; index < candidate.size(); index++) {
            if (candidateMatched[index]) {
                continue;
            }
            BlockNode block = candidate.get(index);
            String path = parentPath + "/nodes[" + index + "]";
            String summary = path + ": added '" + truncate(block.text()) + "'";
            added.add(path);
            entries.add(new ChangeDiffHumanReadableEntry("ADDED", path, summary));
        }
    }

    private void matchAndEmit(
            String parentPath,
            List<BlockNode> baseline,
            List<BlockNode> candidate,
            boolean[] baselineMatched,
            boolean[] candidateMatched,
            boolean exactPass,
            List<ChangeDiffHumanReadableEntry> entries,
            List<String> added,
            List<String> removed,
            List<ChangeDiffModificationView> modified
    ) {
        List<BlockNode> baselineOpen = new ArrayList<>();
        List<Integer> baselineIndexes = new ArrayList<>();
        for (int i = 0; i < baseline.size(); i++) {
            if (!baselineMatched[i]) {
                baselineOpen.add(baseline.get(i));
                baselineIndexes.add(i);
            }
        }
        List<BlockNode> candidateOpen = new ArrayList<>();
        List<Integer> candidateIndexes = new ArrayList<>();
        for (int j = 0; j < candidate.size(); j++) {
            if (!candidateMatched[j]) {
                candidateOpen.add(candidate.get(j));
                candidateIndexes.add(j);
            }
        }
        int[][] lcs = buildLcsTable(baselineOpen, candidateOpen, exactPass);
        int i = baselineOpen.size();
        int j = candidateOpen.size();
        List<int[]> openPairs = new ArrayList<>();
        while (i > 0 && j > 0) {
            if (nodesAlign(baselineOpen.get(i - 1), candidateOpen.get(j - 1), exactPass, baselineOpen, candidateOpen)) {
                openPairs.add(new int[] {i - 1, j - 1});
                i--;
                j--;
            } else if (lcs[i - 1][j] >= lcs[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        for (int[] openPair : openPairs) {
            int baselineIndex = baselineIndexes.get(openPair[0]);
            int candidateIndex = candidateIndexes.get(openPair[1]);
            baselineMatched[baselineIndex] = true;
            candidateMatched[candidateIndex] = true;
            BlockNode from = baseline.get(baselineIndex);
            BlockNode to = candidate.get(candidateIndex);
            String fromPath = parentPath + "/nodes[" + baselineIndex + "]";
            String toPath = parentPath + "/nodes[" + candidateIndex + "]";
            if (baselineIndex != candidateIndex && exactPass) {
                String summary = fromPath + " → " + toPath + ": moved '" + truncate(from.text()) + "'";
                modified.add(new ChangeDiffModificationView(toPath, "MOVED", summary));
                entries.add(new ChangeDiffHumanReadableEntry("MOVED", toPath, summary));
            }
            if (!exactPass && !Objects.equals(from.text(), to.text())) {
                String summary = toPath + ": '" + truncate(from.text()) + "' → '" + truncate(to.text()) + "'";
                modified.add(new ChangeDiffModificationView(toPath, "MODIFIED", summary));
                entries.add(new ChangeDiffHumanReadableEntry("MODIFIED", toPath, summary));
            }
            if (!from.children().isEmpty() || !to.children().isEmpty()) {
                diffBlockLists(toPath, from.children(), to.children(), entries, added, removed, modified);
            }
        }
    }

    private int[][] buildLcsTable(List<BlockNode> baseline, List<BlockNode> candidate, boolean exactPass) {
        int[][] table = new int[baseline.size() + 1][candidate.size() + 1];
        for (int i = 1; i <= baseline.size(); i++) {
            for (int j = 1; j <= candidate.size(); j++) {
                if (nodesAlign(baseline.get(i - 1), candidate.get(j - 1), exactPass, baseline, candidate)) {
                    table[i][j] = table[i - 1][j - 1] + 1;
                } else {
                    table[i][j] = Math.max(table[i - 1][j], table[i][j - 1]);
                }
            }
        }
        return table;
    }

    private boolean nodesAlign(
            BlockNode left,
            BlockNode right,
            boolean exactPass,
            List<BlockNode> baselineOpen,
            List<BlockNode> candidateOpen
    ) {
        if (exactPass) {
            return left.exactKey().equals(right.exactKey());
        }
        if (!left.softKey().equals(right.softKey())) {
            return false;
        }
        long sameSoftBaseline = baselineOpen.stream().filter(node -> node.softKey().equals(left.softKey())).count();
        long sameSoftCandidate = candidateOpen.stream().filter(node -> node.softKey().equals(right.softKey())).count();
        if (sameSoftBaseline == 1 && sameSoftCandidate == 1
                && Math.max(left.text().length(), right.text().length()) <= 4) {
            return true;
        }
        return textSimilar(left.text(), right.text());
    }

    private boolean textSimilar(String left, String right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return false;
        }
        int sharedPrefix = 0;
        int limit = Math.min(left.length(), right.length());
        while (sharedPrefix < limit && left.charAt(sharedPrefix) == right.charAt(sharedPrefix)) {
            sharedPrefix++;
        }
        int minLen = Math.min(left.length(), right.length());
        int maxLen = Math.max(left.length(), right.length());
        return sharedPrefix >= Math.min(4, minLen) || ((double) sharedPrefix / maxLen) >= 0.4d;
    }

    private JsonNode parseNodes(String structuredContentJson) {
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (root == null || !root.isObject() || !root.has("nodes") || !root.get("nodes").isArray()) {
                throw new TemplateValidationException("api.error.template.structuredContentInvalid");
            }
            return root.get("nodes");
        } catch (TemplateValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.template.structuredContentInvalid");
        }
    }

    private List<BlockNode> toBlocks(JsonNode nodes) {
        List<BlockNode> blocks = new ArrayList<>();
        if (nodes == null || !nodes.isArray()) {
            return blocks;
        }
        for (JsonNode node : nodes) {
            if (!node.isObject()) {
                continue;
            }
            String type = node.path("type").asText("");
            StructuredContentNodeType nodeType = StructuredContentNodeType.fromJsonType(type).orElse(null);
            if (nodeType == null || nodeType.category() != StructuredContentNodeType.NodeCategory.BLOCK) {
                continue;
            }
            List<BlockNode> children = List.of();
            JsonNode childNodes = node.get("children");
            if (childNodes != null && childNodes.isArray() && hasBlockChildren(childNodes)) {
                children = toBlocks(childNodes);
            }
            blocks.add(new BlockNode(type, extractText(node), structuralSignature(node), children));
        }
        return blocks;
    }

    private boolean hasBlockChildren(JsonNode children) {
        for (JsonNode child : children) {
            StructuredContentNodeType type = StructuredContentNodeType.fromJsonType(child.path("type").asText(null))
                    .orElse(null);
            if (type != null && type.category() == StructuredContentNodeType.NodeCategory.BLOCK) {
                return true;
            }
        }
        return false;
    }

    private String structuralSignature(JsonNode node) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if ("children".equals(name) || "type".equals(name)) {
                continue;
            }
            JsonNode value = node.get(name);
            if (value != null && value.isValueNode()) {
                builder.append(name).append('=').append(value.asText()).append(';');
            }
        }
        return builder.toString();
    }

    private String extractText(JsonNode node) {
        StringBuilder builder = new StringBuilder();
        collectInlineText(node.get("children"), builder);
        return builder.toString().trim();
    }

    private void collectInlineText(JsonNode node, StringBuilder builder) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            String type = node.path("type").asText("");
            StructuredContentNodeType nodeType = StructuredContentNodeType.fromJsonType(type).orElse(null);
            if (nodeType != null && nodeType.category() == StructuredContentNodeType.NodeCategory.BLOCK) {
                return;
            }
            if ("textRun".equals(type) || "text".equals(type)) {
                String value = node.path("value").asText("");
                if (!value.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(value);
                }
            }
            JsonNode children = node.get("children");
            if (children != null) {
                collectInlineText(children, builder);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectInlineText(child, builder);
            }
        }
    }

    static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('\n', ' ').trim();
        if (normalized.length() <= TEXT_SNIPPET_MAX) {
            return normalized;
        }
        return normalized.substring(0, TEXT_SNIPPET_MAX) + "…";
    }

    record Result(
            List<String> added,
            List<String> removed,
            List<ChangeDiffModificationView> modified,
            List<ChangeDiffHumanReadableEntry> entries
    ) {
    }

    private record BlockNode(
            String type,
            String text,
            String structureSignature,
            List<BlockNode> children
    ) {
        String exactKey() {
            return type + "|" + text + "|" + structureSignature;
        }

        String softKey() {
            return type + "|" + structureSignature;
        }
    }
}
