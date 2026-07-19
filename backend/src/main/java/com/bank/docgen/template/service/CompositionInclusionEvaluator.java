package com.bank.docgen.template.service;

import com.bank.docgen.template.api.CompositionInclusionMatchView;
import com.bank.docgen.template.api.CompositionInclusionRuleView;
import com.bank.docgen.template.api.CompositionInclusionSummaryEntryView;
import com.bank.docgen.template.domain.CompositionInclusionDecision;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ADR-0063 deterministic Composition Inclusion Rule evaluator (IBL-E2).
 */
public final class CompositionInclusionEvaluator {

    public static final String NONE_DEFAULT = "NONE_DEFAULT";

    private CompositionInclusionEvaluator() {
    }

    public static EvaluationResult evaluate(
            List<String> referenceKeys,
            List<CompositionInclusionRuleView> rules,
            CompositionInclusionAxes axes
    ) {
        CompositionInclusionAxes normalizedAxes = axes == null ? CompositionInclusionAxes.empty() : axes;
        List<CompositionInclusionRuleView> safeRules = rules == null ? List.of() : rules;
        Map<String, List<CompositionInclusionRuleView>> byKey = new LinkedHashMap<>();
        for (CompositionInclusionRuleView rule : safeRules) {
            if (rule == null || rule.referenceKey() == null || rule.referenceKey().isBlank()) {
                continue;
            }
            byKey.computeIfAbsent(rule.referenceKey(), ignored -> new ArrayList<>()).add(rule);
        }

        List<CompositionInclusionSummaryEntryView> decisions = new ArrayList<>();
        Set<String> included = new LinkedHashSet<>();
        for (String referenceKey : referenceKeys == null ? List.<String>of() : referenceKeys) {
            List<CompositionInclusionRuleView> targeting = byKey.getOrDefault(referenceKey, List.of());
            if (targeting.isEmpty()) {
                decisions.add(new CompositionInclusionSummaryEntryView(
                        referenceKey,
                        CompositionInclusionDecision.INCLUDE,
                        NONE_DEFAULT
                ));
                included.add(referenceKey);
                continue;
            }
            List<CompositionInclusionRuleView> ordered = targeting.stream()
                    .sorted(Comparator
                            .comparingInt(CompositionInclusionRuleView::resolvedPriority)
                            .thenComparing(rule -> rule.ruleId() == null ? "" : rule.ruleId()))
                    .toList();
            CompositionInclusionRuleView matched = null;
            for (CompositionInclusionRuleView rule : ordered) {
                if (matches(rule.match(), normalizedAxes)) {
                    matched = rule;
                    break;
                }
            }
            if (matched != null) {
                decisions.add(new CompositionInclusionSummaryEntryView(
                        referenceKey,
                        CompositionInclusionDecision.INCLUDE,
                        matched.ruleId()
                ));
                included.add(referenceKey);
                continue;
            }
            boolean required = ordered.stream().anyMatch(CompositionInclusionRuleView::resolvedRequiredInclusion);
            if (required) {
                throw new CompositionInclusionUnsatisfiedException();
            }
            decisions.add(new CompositionInclusionSummaryEntryView(
                    referenceKey,
                    CompositionInclusionDecision.EXCLUDE,
                    ordered.getFirst().ruleId()
            ));
        }
        return new EvaluationResult(List.copyOf(decisions), Set.copyOf(included));
    }

    public static void assertJurisdictionCompatible(String cmJurisdiction, String contextJurisdiction) {
        String left = CompositionInclusionAxes.of(cmJurisdiction, null, null).jurisdiction();
        String right = CompositionInclusionAxes.of(contextJurisdiction, null, null).jurisdiction();
        if (left == null || right == null) {
            return;
        }
        if (!left.equalsIgnoreCase(right)) {
            throw new ContentModuleJurisdictionMismatchException();
        }
    }

    private static boolean matches(CompositionInclusionMatchView match, CompositionInclusionAxes axes) {
        if (match == null) {
            return false;
        }
        String jurisdiction = normalizeAxis(match.jurisdiction());
        String product = normalizeAxis(match.product());
        String channel = normalizeAxis(match.channel());
        if (jurisdiction == null && product == null && channel == null) {
            return false;
        }
        if (jurisdiction != null && !axisEquals(jurisdiction, axes.jurisdiction())) {
            return false;
        }
        if (product != null && !axisEquals(product, axes.product())) {
            return false;
        }
        if (channel != null && !axisEquals(channel, axes.channel())) {
            return false;
        }
        return true;
    }

    private static boolean axisEquals(String expected, String actual) {
        return actual != null && expected.equalsIgnoreCase(actual);
    }

    private static String normalizeAxis(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record EvaluationResult(
            List<CompositionInclusionSummaryEntryView> decisions,
            Set<String> includedReferenceKeys
    ) {
        public EvaluationResult {
            decisions = decisions == null ? List.of() : List.copyOf(decisions);
            includedReferenceKeys = includedReferenceKeys == null ? Set.of() : Set.copyOf(includedReferenceKeys);
        }

        public Set<String> includedReferenceKeys() {
            return includedReferenceKeys;
        }
    }
}
