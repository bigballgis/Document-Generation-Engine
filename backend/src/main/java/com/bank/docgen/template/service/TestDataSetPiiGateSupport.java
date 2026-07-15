package com.bank.docgen.template.service;

import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.domain.TestDataSetPiiHandling;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fail-closed PII handling gate for test-data-set create/update (CE-G03).
 */
final class TestDataSetPiiGateSupport {

    static final String MESSAGE_HANDLING_REQUIRED = "api.error.template.testDataSetPiiHandlingRequired";
    static final String MESSAGE_REASON_REQUIRED = "api.error.template.piiConfirmReasonRequired";
    static final String MESSAGE_SECONDARY_REQUIRED = "api.error.template.piiSecondaryConfirmRequired";
    private static final int REASON_MAX_LENGTH = 2048;

    private TestDataSetPiiGateSupport() {
    }

    static List<VariableSchemaEntity> triggeredPiiFields(
            List<VariableSchemaEntity> schema,
            Map<String, Object> variables
    ) {
        List<VariableSchemaEntity> triggered = new ArrayList<>();
        for (VariableSchemaEntity variable : schema) {
            if (variable.getPiiCategory() == null || variable.getPiiCategory() == VariablePiiCategory.NONE) {
                continue;
            }
            if (TestDataSetVariablesSchemaValidator.isCompute(variable)) {
                continue;
            }
            if (hasNonEmptyValue(variables, variable.getVariableKey())) {
                triggered.add(variable);
            }
        }
        return List.copyOf(triggered);
    }

    static TestDataSetPiiHandling requireHandling(
            UpsertTestDataSetRequest request,
            List<VariableSchemaEntity> triggeredPiiFields
    ) {
        if (triggeredPiiFields.isEmpty()) {
            return null;
        }
        TestDataSetPiiHandling handling = parseHandling(request.piiHandling());
        if (handling == null) {
            throw new TemplateValidationException(MESSAGE_HANDLING_REQUIRED);
        }
        if (handling == TestDataSetPiiHandling.EXPLICIT_SENSITIVE) {
            String reason = request.piiConfirmReason();
            if (reason == null || reason.isBlank()) {
                throw new TemplateValidationException(MESSAGE_REASON_REQUIRED);
            }
            if (reason.length() > REASON_MAX_LENGTH) {
                throw new TemplateValidationException(MESSAGE_REASON_REQUIRED);
            }
            if (!Boolean.TRUE.equals(request.secondaryConfirmed())) {
                throw new TemplateValidationException(MESSAGE_SECONDARY_REQUIRED);
            }
        }
        return handling;
    }

    static Map<String, String> categoryByKey(List<VariableSchemaEntity> triggeredPiiFields) {
        Map<String, String> categories = new LinkedHashMap<>();
        for (VariableSchemaEntity variable : triggeredPiiFields) {
            categories.put(variable.getVariableKey(), variable.getPiiCategory().name());
        }
        return Map.copyOf(categories);
    }

    static List<String> keys(List<VariableSchemaEntity> triggeredPiiFields) {
        return triggeredPiiFields.stream().map(VariableSchemaEntity::getVariableKey).toList();
    }

    private static TestDataSetPiiHandling parseHandling(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TestDataSetPiiHandling.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean hasNonEmptyValue(Map<String, Object> variables, String key) {
        if (variables == null || !variables.containsKey(key)) {
            return false;
        }
        Object value = variables.get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        return true;
    }
}
