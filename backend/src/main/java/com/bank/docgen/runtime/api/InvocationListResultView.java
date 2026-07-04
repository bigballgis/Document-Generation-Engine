package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record InvocationListResultView(
        String view,
        List<InvocationSummaryView> items,
        int page,
        int size,
        long totalElements
) {
    public InvocationListResultView {
        items = DefensiveCopies.copyList(items);
    }

}
