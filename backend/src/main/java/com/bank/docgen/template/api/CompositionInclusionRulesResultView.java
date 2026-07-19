package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record CompositionInclusionRulesResultView(
        List<CompositionInclusionRuleView> rules
) {
    public CompositionInclusionRulesResultView {
        rules = DefensiveCopies.copyList(rules);
    }
}
