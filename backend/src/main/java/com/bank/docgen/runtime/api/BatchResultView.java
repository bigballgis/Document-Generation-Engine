package com.bank.docgen.runtime.api;

import java.util.List;

public record BatchResultView(
        String batchId,
        BatchSummaryView summary,
        List<BatchResultItemView> items
) {
}
