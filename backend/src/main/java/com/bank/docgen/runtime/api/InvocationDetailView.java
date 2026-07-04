package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;
import java.util.Map;

public record InvocationDetailView(
        InvocationSummaryView summary,
        Map<String, Object> parameters,
        List<InvocationSummaryView> childItems
) {
    public InvocationDetailView {
        parameters = DefensiveCopies.copyMap(parameters);
        childItems = DefensiveCopies.copyList(childItems);
    }

}
