package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;
import java.util.Map;

public record RiskPromptConfigView(
        String scopeType,
        String groupCode,
        List<String> reasonCategories,
        Map<String, String> riskPromptCopy,
        String updatedAt
) {
    public RiskPromptConfigView {
        reasonCategories = DefensiveCopies.copyList(reasonCategories);
        riskPromptCopy = DefensiveCopies.copyMap(riskPromptCopy);
    }

}
