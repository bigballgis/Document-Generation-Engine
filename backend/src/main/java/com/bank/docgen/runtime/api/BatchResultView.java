package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchResultView(
        String batchId,
        BatchSummaryView summary,
        List<BatchResultItemView> items,
        String originalBatchId
) {
    public BatchResultView {
        items = DefensiveCopies.copyList(items);
    }

    public BatchResultView(String batchId, BatchSummaryView summary, List<BatchResultItemView> items) {
        this(batchId, summary, items, null);
    }
}
