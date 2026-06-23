package com.bank.docgen.template.api;

import java.util.List;

public record TemplateRuleValidationItemRequest(
        String ruleId,
        String conditionExpression,
        String targetAnchorId,
        String trueBranchRuleId,
        String falseBranchRuleId
) {
}
