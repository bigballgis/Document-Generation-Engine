package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record BindingValidationView(
        List<AnchorBindingView> bindings,
        BindingValidationSummaryView summary
) {
    public BindingValidationView {
        bindings = DefensiveCopies.copyList(bindings);
    }

}
