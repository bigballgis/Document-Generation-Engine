package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;
import java.util.Map;

public record TemplateRiskPromptConfigView(
        boolean useDefault,
        List<String> reasonCategories,
        Map<String, String> riskPromptCopy,
        String updatedAt
) {
    public TemplateRiskPromptConfigView {
        reasonCategories = DefensiveCopies.copyList(reasonCategories);
        riskPromptCopy = DefensiveCopies.copyMap(riskPromptCopy);
    }

}
