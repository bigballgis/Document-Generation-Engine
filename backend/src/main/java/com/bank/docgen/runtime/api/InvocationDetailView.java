package com.bank.docgen.runtime.api;

import java.util.List;
import java.util.Map;

public record InvocationDetailView(
        InvocationSummaryView summary,
        Map<String, Object> parameters,
        List<InvocationSummaryView> childItems
) {
}
