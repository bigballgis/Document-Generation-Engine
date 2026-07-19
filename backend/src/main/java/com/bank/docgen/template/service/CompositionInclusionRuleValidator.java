package com.bank.docgen.template.service;

import com.bank.docgen.template.api.CompositionInclusionMatchView;
import com.bank.docgen.template.api.CompositionInclusionRuleView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ADR-0063 management PUT validation for Composition Inclusion Rules.
 */
public final class CompositionInclusionRuleValidator {

    private static final int MAX_AXIS_LENGTH = 128;

    private CompositionInclusionRuleValidator() {
    }

    public static void validate(List<CompositionInclusionRuleView> rules, Set<String> declaredReferenceKeys) {
        Set<String> knownKeys = declaredReferenceKeys == null ? Set.of() : declaredReferenceKeys;
        Set<String> seenRuleIds = new HashSet<>();
        List<CompositionInclusionRuleView> safeRules = rules == null ? List.of() : rules;
        for (CompositionInclusionRuleView rule : safeRules) {
            if (rule == null) {
                throw new CompositionInclusionRuleInvalidException("api.error.template.compositionInclusionRuleInvalid");
            }
            String ruleId = normalizeRequired(rule.ruleId());
            if (ruleId == null) {
                throw new CompositionInclusionRuleInvalidException("api.error.template.compositionInclusionRuleInvalid");
            }
            if (!seenRuleIds.add(ruleId)) {
                throw new CompositionInclusionRuleInvalidException("api.error.template.compositionInclusionRuleInvalid");
            }
            String referenceKey = normalizeRequired(rule.referenceKey());
            if (referenceKey == null || !knownKeys.contains(referenceKey)) {
                throw new CompositionInclusionRuleInvalidException("api.error.template.compositionInclusionRuleInvalid");
            }
            CompositionInclusionMatchView match = rule.match();
            if (match == null || !hasAtLeastOneAxis(match)) {
                throw new CompositionInclusionRuleInvalidException("api.error.template.compositionInclusionRuleInvalid");
            }
            assertAxisLength(match.jurisdiction());
            assertAxisLength(match.product());
            assertAxisLength(match.channel());
        }
    }

    private static boolean hasAtLeastOneAxis(CompositionInclusionMatchView match) {
        return normalizeAxis(match.jurisdiction()) != null
                || normalizeAxis(match.product()) != null
                || normalizeAxis(match.channel()) != null;
    }

    private static void assertAxisLength(String value) {
        String normalized = normalizeAxis(value);
        if (normalized != null && normalized.length() > MAX_AXIS_LENGTH) {
            throw new CompositionInclusionRuleInvalidException("api.error.template.compositionInclusionRuleInvalid");
        }
    }

    private static String normalizeRequired(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeAxis(String value) {
        return normalizeRequired(value);
    }
}
