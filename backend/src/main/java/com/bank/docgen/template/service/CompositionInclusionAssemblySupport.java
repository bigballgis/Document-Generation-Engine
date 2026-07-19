package com.bank.docgen.template.service;

import com.bank.docgen.template.api.CompositionInclusionRuleView;
import com.bank.docgen.template.api.CompositionInclusionSummaryEntryView;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies ADR-0063 inclusion evaluation to pinned CM structures (runtime + preview/test).
 */
public final class CompositionInclusionAssemblySupport {

    private CompositionInclusionAssemblySupport() {
    }

    public static AppliedInclusion apply(
            TemplateVersionEntity version,
            Map<String, String> pinnedStructures,
            Map<String, String> pinnedJurisdictions,
            List<CompositionInclusionRuleView> rules,
            CompositionInclusionAxes axes
    ) {
        List<String> referenceKeys = new ArrayList<>(pinnedStructures.keySet());
        CompositionInclusionEvaluator.EvaluationResult evaluation =
                CompositionInclusionEvaluator.evaluate(referenceKeys, rules, axes);
        Map<String, String> included = new LinkedHashMap<>();
        for (String key : evaluation.includedReferenceKeys()) {
            String structure = pinnedStructures.get(key);
            if (structure != null) {
                included.put(key, structure);
            }
            CompositionInclusionEvaluator.assertJurisdictionCompatible(
                    pinnedJurisdictions == null ? null : pinnedJurisdictions.get(key),
                    axes == null ? null : axes.jurisdiction()
            );
        }
        return new AppliedInclusion(included, evaluation.decisions());
    }

    public record AppliedInclusion(
            Map<String, String> pinnedStructures,
            List<CompositionInclusionSummaryEntryView> summary
    ) {
    }
}
