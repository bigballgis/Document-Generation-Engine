package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record UpsertTemplateRiskPromptConfigRequest(
        @NotNull Boolean useDefault,
        List<@Size(max = 64) String> reasonCategories,
        Map<@Size(max = 64) String, @Size(max = 2048) String> riskPromptCopy
) {
    public UpsertTemplateRiskPromptConfigRequest {
        reasonCategories = DefensiveCopies.copyStringList(reasonCategories);
        riskPromptCopy = DefensiveCopies.copyMap(riskPromptCopy);
    }

}
