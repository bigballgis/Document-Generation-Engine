package com.bank.docgen.authoring.structured;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Validates structured v1 table components and builds render-ready models (P18-T04).
 */
@Service
public class TableComponentService {

    public static final String MESSAGE_KEY_NESTED_TABLE = "generation.warning.fidelity.nestedTable";
    public static final String MESSAGE_KEY_UNRELIABLE_TABLE_LAYOUT =
            "generation.warning.fidelity.unreliableTableLayout";
    public static final String MESSAGE_KEY_INVALID_TABLE_COMPONENT =
            "generation.warning.fidelity.invalidTableComponent";

    private final ObjectMapper objectMapper;
    private final TableComponentValidationSupport validation;

    public TableComponentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.validation = new TableComponentValidationSupport();
    }

    public TableComponentValidationResult validateAndBuildRenderModel(String tableComponentJson) {
        try {
            JsonNode root = objectMapper.readTree(tableComponentJson);
            return validation.validateAndBuildRenderModel(root);
        } catch (IOException ex) {
            return validation.invalidDefinitionResult();
        }
    }

    public StructuredContentValidationResult validateStructuredContent(String structuredContentJson) {
        return validateStructuredContent(structuredContentJson, Set.of());
    }

    public StructuredContentValidationResult validateStructuredContent(
            String structuredContentJson,
            Set<String> declaredVariableKeys
    ) {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (root.isObject() && root.get("nodes").isArray()) {
                validation.walkContentNodes(
                        root.get("nodes"),
                        "nodes",
                        declaredVariableKeys,
                        blockers,
                        this::validateAndBuildRenderModel
                );
            }
        } catch (IOException ex) {
            return StructuredContentValidationResult.of(blockers, List.of());
        }
        return StructuredContentValidationResult.of(blockers, List.of());
    }
}
