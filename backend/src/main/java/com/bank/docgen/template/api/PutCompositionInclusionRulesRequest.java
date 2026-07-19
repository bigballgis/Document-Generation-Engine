package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PutCompositionInclusionRulesRequest(
        @NotNull @Valid List<CompositionInclusionRuleView> rules
) {
    public PutCompositionInclusionRulesRequest {
        rules = DefensiveCopies.copyList(rules);
    }
}
