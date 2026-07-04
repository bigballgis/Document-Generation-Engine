package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record BatchResultView(
        String batchId,
        BatchSummaryView summary,
        List<BatchResultItemView> items
) {
    public BatchResultView {
        items = DefensiveCopies.copyList(items);
    }
}
