package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.RuleValidationStatus;

public record TemplateRuleValidationItemResponse(
        String ruleId,
        String conditionExpression,
        String targetAnchorId,
        String trueBranchRuleId,
        String falseBranchRuleId,
        RuleValidationStatus status
) {
}
