package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Validates controlled multi-level numbering and deterministic loop re-sequencing (P18-T06).
 */
@Service
public class NumberingService {

    public static final String MESSAGE_KEY_DUPLICATE_NUMBER = "generation.warning.fidelity.duplicateNumber";
    public static final String MESSAGE_KEY_BROKEN_CROSS_REFERENCE =
            "generation.warning.fidelity.brokenNumberCrossReference";

    private static final int MAX_LEVELS = 4;

    private final ObjectMapper objectMapper;

    public NumberingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NumberingValidationResult validateStructuredContent(String structuredContentJson) {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        List<NumberingSequenceEntry> sequence = new ArrayList<>();
        List<CrossReferenceCheck> crossReferences = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (root.isObject() && root.get("nodes").isArray()) {
                int[] counters = new int[MAX_LEVELS];
                walkNodes(root.get("nodes"), "nodes", counters, sequence, crossReferences);
            }
        } catch (IOException ex) {
            return NumberingValidationResult.of(
                    StructuredContentValidationResult.of(blockers, List.of()),
                    sequence
            );
        }
        detectDuplicateNumbers(sequence, blockers);
        detectBrokenCrossReferences(sequence, crossReferences, blockers);
        return NumberingValidationResult.of(
                StructuredContentValidationResult.of(blockers, List.of()),
                sequence
        );
    }

    public List<String> computeDisplayNumbers(String structuredContentJson) {
        return validateStructuredContent(structuredContentJson).sequence().stream()
                .map(NumberingSequenceEntry::displayNumber)
                .toList();
    }

    private void walkNodes(
            JsonNode nodes,
            String location,
            int[] counters,
            List<NumberingSequenceEntry> sequence,
            List<CrossReferenceCheck> crossReferences
    ) {
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            String nodeLocation = location + "[" + index + "]";
            if (!node.isObject()) {
                continue;
            }
            JsonNode numbering = node.get("numbering");
            if (numbering != null && numbering.isObject()) {
                assignNumber(nodeLocation, numbering, counters, sequence);
            }
            JsonNode crossRef = node.get("numberingCrossRef");
            if (crossRef != null && crossRef.isObject()) {
                crossReferences.add(new CrossReferenceCheck(
                        nodeLocation,
                        crossRef.path("targetNumber").asText("").trim()
                ));
            }
            if ("loopBlock".equals(node.path("type").asText())) {
                int iterations = node.path("validationIterations").asInt(2);
                JsonNode children = node.get("children");
                if (children != null && children.isArray()) {
                    for (int iteration = 0; iteration < iterations; iteration++) {
                        walkNodes(children, nodeLocation + ".children[loop:" + iteration + "]", counters, sequence, crossReferences);
                    }
                }
            } else {
                JsonNode children = node.get("children");
                if (children != null && children.isArray()) {
                    walkNodes(children, nodeLocation + ".children", counters, sequence, crossReferences);
                }
            }
        }
    }

    private void assignNumber(
            String location,
            JsonNode numbering,
            int[] counters,
            List<NumberingSequenceEntry> sequence
    ) {
        int level = numbering.path("level").asInt(1);
        if (level < 1 || level > MAX_LEVELS) {
            return;
        }
        String explicit = numbering.path("displayNumber").asText("").trim();
        String displayNumber;
        if (!explicit.isBlank()) {
            displayNumber = explicit;
        } else {
            counters[level - 1]++;
            for (int deeper = level; deeper < MAX_LEVELS; deeper++) {
                counters[deeper] = 0;
            }
            displayNumber = buildDisplayNumber(counters, level);
        }
        sequence.add(new NumberingSequenceEntry(location, displayNumber, level));
    }

    private String buildDisplayNumber(int[] counters, int level) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < level; index++) {
            if (index > 0) {
                builder.append('.');
            }
            builder.append(counters[index]);
        }
        return builder.toString();
    }

    private void detectDuplicateNumbers(
            List<NumberingSequenceEntry> sequence,
            List<StructuredContentFidelityIssue> blockers
    ) {
        Map<String, String> firstLocationByNumber = new HashMap<>();
        for (NumberingSequenceEntry entry : sequence) {
            String existing = firstLocationByNumber.putIfAbsent(entry.displayNumber(), entry.location());
            if (existing != null) {
                blockers.add(issue(
                        FidelityWarningCode.DUPLICATE_NUMBER,
                        MESSAGE_KEY_DUPLICATE_NUMBER,
                        entry.location(),
                        "Duplicate numbering '" + entry.displayNumber() + "' also assigned at " + existing + ".",
                        "Ensure each numbered heading has a unique display number."
                ));
            }
        }
    }

    private void detectBrokenCrossReferences(
            List<NumberingSequenceEntry> sequence,
            List<CrossReferenceCheck> crossReferences,
            List<StructuredContentFidelityIssue> blockers
    ) {
        Set<String> assignedNumbers = new HashSet<>();
        sequence.forEach(entry -> assignedNumbers.add(entry.displayNumber()));
        for (CrossReferenceCheck crossReference : crossReferences) {
            if (crossReference.targetNumber().isBlank()) {
                continue;
            }
            if (!assignedNumbers.contains(crossReference.targetNumber())) {
                blockers.add(issue(
                        FidelityWarningCode.BROKEN_NUMBER_CROSS_REFERENCE,
                        MESSAGE_KEY_BROKEN_CROSS_REFERENCE,
                        crossReference.location(),
                        "Number cross-reference '" + crossReference.targetNumber() + "' is unresolved.",
                        "Reference an existing numbered heading or clause number."
                ));
            }
        }
    }

    private StructuredContentFidelityIssue issue(
            FidelityWarningCode code,
            String messageKey,
            String location,
            String detectionSummary,
            String suggestion
    ) {
        return new StructuredContentFidelityIssue(
                StructuredContentFidelitySeverity.BLOCKER,
                code,
                messageKey,
                location,
                detectionSummary,
                suggestion
        );
    }

    private record CrossReferenceCheck(String location, String targetNumber) {
    }
}
