package com.bank.docgen.template.api;

import java.util.List;

public record TemplateRuleValidationView(
        boolean validated,
        List<TemplateRuleValidationItemResponse> rules,
        TemplateRuleValidationSummaryView summary
) {
}
