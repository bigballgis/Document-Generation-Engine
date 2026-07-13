package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fail-closed validation of test-data-set variables against template VariableSchema (CE-U03).
 */
final class TestDataSetVariablesSchemaValidator {

    private TestDataSetVariablesSchemaValidator() {
    }

    static boolean isCompute(VariableSchemaEntity variable) {
        if (variable.getVariableType() == VariableType.COMPUTED) {
            return true;
        }
        String expression = variable.getComputeExpression();
        return expression != null && !expression.isBlank();
    }

    static Map<String, Object> stripComputeKeys(
            List<VariableSchemaEntity> schema,
            Map<String, Object> variables
    ) {
        Set<String> computeKeys = schema.stream()
                .filter(TestDataSetVariablesSchemaValidator::isCompute)
                .map(VariableSchemaEntity::getVariableKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Object> cleaned = new LinkedHashMap<>();
        if (variables == null) {
            return cleaned;
        }
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (!computeKeys.contains(entry.getKey())) {
                cleaned.put(entry.getKey(), entry.getValue());
            }
        }
        return cleaned;
    }

    static List<FieldError> validate(List<VariableSchemaEntity> schema, Map<String, Object> variables) {
        List<FieldError> errors = new ArrayList<>();
        Map<String, Object> payload = variables == null ? Map.of() : variables;
        Set<String> knownKeys = schema.stream()
                .map(VariableSchemaEntity::getVariableKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (VariableSchemaEntity variable : schema) {
            if (isCompute(variable)) {
                continue;
            }
            String key = variable.getVariableKey();
            Object value = payload.get(key);
            boolean missing = !payload.containsKey(key) || isEmpty(value);
            if (variable.isRequired() && missing) {
                errors.add(new FieldError(key, "REQUIRED", "Field is required."));
                continue;
            }
            if (missing) {
                continue;
            }
            FieldError typeError = validateType(variable, value);
            if (typeError != null) {
                errors.add(typeError);
            }
        }

        for (String key : payload.keySet()) {
            if (!knownKeys.contains(key)) {
                errors.add(new FieldError(key, "UNKNOWN_FIELD", "Unknown field."));
            }
        }
        return errors;
    }

    private static FieldError validateType(VariableSchemaEntity variable, Object value) {
        String key = variable.getVariableKey();
        VariableType type = variable.getVariableType();
        return switch (type) {
            case NUMBER, AMOUNT -> isNumber(value)
                    ? null
                    : new FieldError(key, "INVALID_TYPE", "Value type is invalid.");
            case BOOLEAN -> value instanceof Boolean
                    ? null
                    : new FieldError(key, "INVALID_TYPE", "Value type is invalid.");
            case DATE -> isDate(value)
                    ? null
                    : new FieldError(key, "INVALID_FORMAT", "Value format is invalid.");
            case ENUM -> isAllowedEnum(variable.getEnumValues(), value)
                    ? null
                    : new FieldError(key, "ENUM_NOT_ALLOWED", "Value is not allowed.");
            case LIST -> value instanceof List<?>
                    ? null
                    : new FieldError(key, "INVALID_TYPE", "Value type is invalid.");
            case OBJECT -> value instanceof Map<?, ?>
                    ? null
                    : new FieldError(key, "INVALID_TYPE", "Value type is invalid.");
            case TEXT, COMPUTED -> value instanceof String || value instanceof Number || value instanceof Boolean
                    ? null
                    : new FieldError(key, "INVALID_TYPE", "Value type is invalid.");
        };
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        return false;
    }

    private static boolean isNumber(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                new BigDecimal(text.trim());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;
    }

    private static boolean isDate(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(text.trim());
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean isAllowedEnum(String enumValues, Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        List<String> allowed = parseEnumValues(enumValues);
        if (allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(text);
    }

    private static List<String> parseEnumValues(String enumValues) {
        if (enumValues == null || enumValues.isBlank()) {
            return List.of();
        }
        String trimmed = enumValues.trim();
        if (trimmed.startsWith("[")) {
            String inner = trimmed.substring(1, Math.max(1, trimmed.length() - 1));
            return Arrays.stream(inner.split(","))
                    .map(item -> item.trim().replace("\"", ""))
                    .filter(item -> !item.isEmpty())
                    .toList();
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    static void validateOrThrow(List<VariableSchemaEntity> schema, Map<String, Object> variables) {
        List<FieldError> errors = validate(schema, variables);
        if (!errors.isEmpty()) {
            throw new TestDataSetSchemaValidationException(errors);
        }
    }
}
