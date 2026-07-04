package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;
import java.util.Map;

public record DecisionFormConfigView(
        List<String> reasonCategories,
        Map<String, String> riskPromptCopy
) {
    public DecisionFormConfigView {
        reasonCategories = DefensiveCopies.copyStringList(reasonCategories);
        riskPromptCopy = DefensiveCopies.copyMap(riskPromptCopy);
    }

}
