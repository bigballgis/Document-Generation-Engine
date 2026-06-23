package com.bank.docgen.template.api;

public record CompositionRuleView(
        String ruleId,
        String conditionExpression,
        String targetAnchorId,
        String trueBranchRuleId,
        String falseBranchRuleId
) {
}
