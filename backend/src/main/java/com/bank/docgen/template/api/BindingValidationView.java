package com.bank.docgen.template.api;

import java.util.List;

public record BindingValidationView(
        List<AnchorBindingView> bindings,
        BindingValidationSummaryView summary
) {
}
