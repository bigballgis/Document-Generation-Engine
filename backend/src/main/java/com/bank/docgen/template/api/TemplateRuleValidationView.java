package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record TemplateRuleValidationView(
        boolean validated,
        List<TemplateRuleValidationItemResponse> rules,
        TemplateRuleValidationSummaryView summary
) {
    public TemplateRuleValidationView {
        rules = DefensiveCopies.copyList(rules);
    }

}
