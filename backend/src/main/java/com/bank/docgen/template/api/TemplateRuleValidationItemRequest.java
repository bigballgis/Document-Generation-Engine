package com.bank.docgen.template.api;

public record TemplateRuleValidationItemRequest(
        String ruleId,
        String conditionExpression,
        String targetAnchorId,
        String trueBranchRuleId,
        String falseBranchRuleId
) {
}
