package com.bank.docgen.runtime.api;

import java.util.List;

public record InvocationListResultView(
        String view,
        List<InvocationSummaryView> items,
        int page,
        int size,
        long totalElements
) {
}
