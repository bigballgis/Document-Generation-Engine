package com.bank.docgen.runtime.service;

import com.bank.docgen.template.domain.VariablePiiCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IBL-A5 / ADR-0057 amendment: redact invocation-parameter variables before
 * {@code parameters_storage} persistence. {@code piiCategory=NONE} may retain cleartext;
 * any other category and unknown keys are excluded (fail-closed).
 */
public final class InvocationRetentionVariableRedactor {

    /** Stable sentinel if a caller chooses to write a placeholder instead of omitting the key. */
    public static final String REDACTED_SENTINEL = "[REDACTED]";

    private InvocationRetentionVariableRedactor() {
    }

    public record Result(
            Map<String, Object> variables,
            List<String> redactedVariableKeys,
            Map<String, String> redactedPiiCategories
    ) {
    }

    /**
     * @param original variables as submitted by the caller (pre-redaction)
     * @param schemaCategories key → category from the resolved template version schema;
     *                         missing keys are treated as sensitive ({@link VariablePiiCategory#OTHER_SENSITIVE})
     */
    public static Result redact(
            Map<String, Object> original,
            Map<String, VariablePiiCategory> schemaCategories
    ) {
        Map<String, VariablePiiCategory> categories =
                schemaCategories == null ? Map.of() : schemaCategories;
        Map<String, Object> retained = new LinkedHashMap<>();
        List<String> redactedKeys = new ArrayList<>();
        Map<String, String> redactedCategories = new LinkedHashMap<>();
        if (original == null || original.isEmpty()) {
            return new Result(retained, List.of(), Map.of());
        }
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            String key = entry.getKey();
            VariablePiiCategory category = categories.get(key);
            if (category == null) {
                redactedKeys.add(key);
                redactedCategories.put(key, VariablePiiCategory.OTHER_SENSITIVE.name());
                continue;
            }
            if (category != VariablePiiCategory.NONE) {
                redactedKeys.add(key);
                redactedCategories.put(key, category.name());
                continue;
            }
            retained.put(key, entry.getValue());
        }
        return new Result(
                Collections.unmodifiableMap(retained),
                List.copyOf(redactedKeys),
                Map.copyOf(redactedCategories)
        );
    }

    /**
     * Variables safe to feed into regenerate assembly: drop nulls and redaction sentinels.
     */
    public static Map<String, Object> toReplayVariables(Map<String, Object> retained) {
        if (retained == null || retained.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> replay = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : retained.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof String text && REDACTED_SENTINEL.equals(text)) {
                continue;
            }
            replay.put(entry.getKey(), value);
        }
        return Map.copyOf(replay);
    }
}
