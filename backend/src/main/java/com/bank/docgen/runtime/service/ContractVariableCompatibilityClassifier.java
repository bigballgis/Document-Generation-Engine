package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.ContractVariableSchemaView;
import com.bank.docgen.runtime.domain.ContractVariableCompatibility;
import com.bank.docgen.template.domain.VariableType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure consumer-contract compatibility classifier for {@code callableVersions[].variables[]}
 * semantic fingerprints (IBL-A4 / A4-C11–C14). Not a publish API hard-gate.
 */
public final class ContractVariableCompatibilityClassifier {

    private ContractVariableCompatibilityClassifier() {
    }

    public static ContractVariableCompatibility classify(
            List<ContractVariableSchemaView> baseline,
            List<ContractVariableSchemaView> candidate
    ) {
        Map<String, ContractVariableSchemaView> baselineByKey = indexByKey(baseline);
        Map<String, ContractVariableSchemaView> candidateByKey = indexByKey(candidate);

        for (Map.Entry<String, ContractVariableSchemaView> entry : baselineByKey.entrySet()) {
            ContractVariableSchemaView candidateField = candidateByKey.get(entry.getKey());
            if (candidateField == null) {
                return ContractVariableCompatibility.BREAKING;
            }
            if (isBreakingFieldChange(entry.getValue(), candidateField)) {
                return ContractVariableCompatibility.BREAKING;
            }
        }
        return ContractVariableCompatibility.NON_BREAKING;
    }

    private static boolean isBreakingFieldChange(
            ContractVariableSchemaView baseline,
            ContractVariableSchemaView candidate
    ) {
        if (baseline.variableType() != candidate.variableType()) {
            return true;
        }
        if (!baseline.required() && candidate.required()) {
            return true;
        }
        if (!baseline.computed() && candidate.computed()) {
            return true;
        }
        return isEnumShrink(baseline, candidate);
    }

    private static boolean isEnumShrink(
            ContractVariableSchemaView baseline,
            ContractVariableSchemaView candidate
    ) {
        if (baseline.variableType() != VariableType.ENUM) {
            return false;
        }
        Set<String> baselineValues = new HashSet<>(enumSet(baseline.enumValues()));
        Set<String> candidateValues = enumSet(candidate.enumValues());
        return !candidateValues.containsAll(baselineValues);
    }

    private static Set<String> enumSet(List<String> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private static Map<String, ContractVariableSchemaView> indexByKey(List<ContractVariableSchemaView> fields) {
        Map<String, ContractVariableSchemaView> byKey = new HashMap<>();
        if (fields == null) {
            return byKey;
        }
        for (ContractVariableSchemaView field : fields) {
            if (field == null || field.variableKey() == null || field.variableKey().isBlank()) {
                continue;
            }
            byKey.put(field.variableKey(), field);
        }
        return byKey;
    }
}
