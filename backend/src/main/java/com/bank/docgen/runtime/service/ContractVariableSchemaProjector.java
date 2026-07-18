package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.ContractVariableSchemaView;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Projects {@link VariableSchemaEntity} rows into caller-safe {@code /contract} variable views.
 */
final class ContractVariableSchemaProjector {

    private ContractVariableSchemaProjector() {
    }

    static List<ContractVariableSchemaView> project(List<VariableSchemaEntity> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return List.of();
        }
        return schemas.stream()
                .map(ContractVariableSchemaProjector::projectOne)
                .sorted(Comparator.comparing(ContractVariableSchemaView::variableKey))
                .toList();
    }

    private static ContractVariableSchemaView projectOne(VariableSchemaEntity schema) {
        boolean computed = schema.getVariableType() == VariableType.COMPUTED
                || (schema.getComputeExpression() != null && !schema.getComputeExpression().isBlank());
        List<String> enumValues = schema.getVariableType() == VariableType.ENUM
                ? parseEnumValues(schema.getEnumValues())
                : null;
        return new ContractVariableSchemaView(
                schema.getVariableKey(),
                schema.getVariableType(),
                schema.isRequired(),
                computed,
                schema.getPiiCategory(),
                enumValues,
                blankToNull(schema.getDescription())
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    static List<String> parseEnumValues(String enumValues) {
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
}
