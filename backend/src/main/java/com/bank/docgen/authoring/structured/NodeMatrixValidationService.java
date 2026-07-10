package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.expression.ConditionExpressionEvaluator;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.sharedkernel.document.structured.WriterUnsupportedStructuredNodeTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Grades structured content trees into publication blockers and fidelity warnings (P18-T02).
 */
@Service
public class NodeMatrixValidationService {

    public static final String MESSAGE_KEY_UNRESOLVED_VARIABLE = "generation.warning.fidelity.unresolvedVariable";
    public static final String MESSAGE_KEY_UNSUPPORTED_NODE = "generation.warning.fidelity.unsupportedNode";
    public static final String MESSAGE_KEY_INVALID_CONDITION_EXPRESSION =
            "generation.warning.fidelity.invalidConditionExpression";

    private final ObjectMapper objectMapper;
    private final ConditionExpressionEvaluator conditionExpressionEvaluator;

    public NodeMatrixValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.conditionExpressionEvaluator = ConditionExpressionEvaluator.INSTANCE;
    }

    public StructuredContentValidationResult validate(String structuredContentJson, Set<String> declaredVariableKeys) {
        Set<String> variableKeys = declaredVariableKeys == null ? Set.of() : declaredVariableKeys;
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        List<StructuredContentFidelityIssue> warnings = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (!root.isObject() || !root.get("nodes").isArray()) {
                blockers.add(invalidDocumentIssue());
                return StructuredContentValidationResult.of(blockers, warnings);
            }
            JsonNode nodes = root.get("nodes");
            for (int index = 0; index < nodes.size(); index++) {
                walkNode(nodes.get(index), "nodes[" + index + "]", variableKeys, blockers, warnings);
            }
        } catch (IOException ex) {
            blockers.add(invalidDocumentIssue());
        }
        return StructuredContentValidationResult.of(blockers, warnings);
    }

    private void walkNode(
            JsonNode node,
            String location,
            Set<String> declaredVariableKeys,
            List<StructuredContentFidelityIssue> blockers,
            List<StructuredContentFidelityIssue> warnings
    ) {
        if (!node.isObject()) {
            blockers.add(invalidDocumentIssue());
            return;
        }
        String rawType = node.path("type").asText("");
        StructuredContentNodeType nodeType = StructuredContentNodeType.fromJsonType(rawType).orElse(null);
        if (nodeType == null) {
            blockers.add(unsupportedNodeIssue(location, rawType));
            return;
        }
        // LR-A4: matrix-declared types without a DOCX writer must block publish/bind early.
        if (WriterUnsupportedStructuredNodeTypes.containsJsonType(rawType)) {
            blockers.add(unsupportedNodeIssue(location, rawType));
        }
        if (nodeType == StructuredContentNodeType.VARIABLE) {
            String variableKey = node.path("key").asText("").trim();
            if (variableKey.isBlank() || !declaredVariableKeys.contains(variableKey)) {
                blockers.add(new StructuredContentFidelityIssue(
                        StructuredContentFidelitySeverity.BLOCKER,
                        FidelityWarningCode.UNRESOLVED_VARIABLE,
                        MESSAGE_KEY_UNRESOLVED_VARIABLE,
                        location,
                        "Variable reference '" + sanitizeForSummary(variableKey) + "' is not declared in the template schema.",
                        "Declare the variable in the template schema or remove the reference."
                ));
            }
        }
        if (nodeType == StructuredContentNodeType.CONDITION_BLOCK) {
            validateConditionExpression(node, location, declaredVariableKeys, blockers);
        }
        if (nodeType == StructuredContentNodeType.LOOP_BLOCK) {
            validateLoopVariable(node, location, declaredVariableKeys, blockers);
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (int index = 0; index < children.size(); index++) {
                walkNode(children.get(index), location + ".children[" + index + "]", declaredVariableKeys, blockers, warnings);
            }
        }
    }

    private void validateConditionExpression(
            JsonNode node,
            String location,
            Set<String> declaredVariableKeys,
            List<StructuredContentFidelityIssue> blockers
    ) {
        String expression = node.path("conditionExpression").asText("").trim();
        if (expression.isBlank() || !conditionExpressionEvaluator.validateSyntax(expression).isEmpty()) {
            blockers.add(invalidConditionExpressionIssue(location, expression));
            return;
        }
        for (String variableKey : conditionExpressionEvaluator.extractVariableReferences(expression)) {
            if (!declaredVariableKeys.contains(variableKey)) {
                blockers.add(new StructuredContentFidelityIssue(
                        StructuredContentFidelitySeverity.BLOCKER,
                        FidelityWarningCode.UNRESOLVED_VARIABLE,
                        MESSAGE_KEY_UNRESOLVED_VARIABLE,
                        location,
                        "Variable reference '" + sanitizeForSummary(variableKey) + "' is not declared in the template schema.",
                        "Declare the variable in the template schema or remove the reference."
                ));
            }
        }
    }

    private void validateLoopVariable(
            JsonNode node,
            String location,
            Set<String> declaredVariableKeys,
            List<StructuredContentFidelityIssue> blockers
    ) {
        String loopVariable = node.path("loopVariable").asText("").trim();
        if (loopVariable.isBlank() || !declaredVariableKeys.contains(loopVariable)) {
            blockers.add(new StructuredContentFidelityIssue(
                    StructuredContentFidelitySeverity.BLOCKER,
                    FidelityWarningCode.UNRESOLVED_VARIABLE,
                    MESSAGE_KEY_UNRESOLVED_VARIABLE,
                    location,
                    "Variable reference '" + sanitizeForSummary(loopVariable) + "' is not declared in the template schema.",
                    "Declare the variable in the template schema or remove the reference."
            ));
        }
    }

    private StructuredContentFidelityIssue invalidConditionExpressionIssue(String location, String expression) {
        return new StructuredContentFidelityIssue(
                StructuredContentFidelitySeverity.BLOCKER,
                FidelityWarningCode.INVALID_CONDITION_EXPRESSION,
                MESSAGE_KEY_INVALID_CONDITION_EXPRESSION,
                location,
                "Condition expression '" + sanitizeForSummary(expression) + "' is malformed.",
                "Fix the condition expression syntax or remove the condition block."
        );
    }

    private StructuredContentFidelityIssue unsupportedNodeIssue(String location, String rawType) {
        return new StructuredContentFidelityIssue(
                StructuredContentFidelitySeverity.BLOCKER,
                FidelityWarningCode.UNSUPPORTED_NODE,
                MESSAGE_KEY_UNSUPPORTED_NODE,
                location,
                "Unsupported node type '" + sanitizeForSummary(rawType) + "' at " + location + ".",
                "Replace the node with a supported v1 node type that has a DOCX writer."
        );
    }

    /**
     * Counts {@link FidelityWarningCode#UNSUPPORTED_NODE} blockers (unknown types and
     * writer-unsupported matrix types). Used by the publish gate dedicated check.
     */
    public int countUnsupportedNodeBlockers(String structuredContentJson) {
        if (structuredContentJson == null || structuredContentJson.isBlank()) {
            return 0;
        }
        return (int) validate(structuredContentJson, Set.of()).blockers().stream()
                .filter(issue -> issue.code() == FidelityWarningCode.UNSUPPORTED_NODE)
                .count();
    }

    private StructuredContentFidelityIssue invalidDocumentIssue() {
        return new StructuredContentFidelityIssue(
                StructuredContentFidelitySeverity.BLOCKER,
                FidelityWarningCode.UNSUPPORTED_NODE,
                MESSAGE_KEY_UNSUPPORTED_NODE,
                "document",
                "Structured content document is invalid or unreadable.",
                "Provide a valid structured content tree with a nodes array."
        );
    }

    private String sanitizeForSummary(String raw) {
        if (raw == null || raw.isBlank()) {
            return "<empty>";
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 64) {
            return trimmed.substring(0, 64) + "...";
        }
        return trimmed;
    }
}
