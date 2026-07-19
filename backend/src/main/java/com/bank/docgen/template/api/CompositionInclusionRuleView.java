package com.bank.docgen.template.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompositionInclusionRuleView(
        String ruleId,
        String referenceKey,
        CompositionInclusionMatchView match,
        Integer priority,
        Boolean requiredInclusion
) {
    public int resolvedPriority() {
        return priority == null ? 0 : priority;
    }

    public boolean resolvedRequiredInclusion() {
        return Boolean.TRUE.equals(requiredInclusion);
    }
}
