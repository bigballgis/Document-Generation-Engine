package com.bank.docgen.contentmodule.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.time.Instant;
import java.util.List;

public record ContentModuleSummaryView(
        String moduleId,
        String moduleCode,
        String groupCode,
        String name,
        String description,
        List<String> sharedGroupCodes,
        String reviewState,
        String lifecycleState,
        Instant createdAt,
        Instant updatedAt
) {
    public ContentModuleSummaryView {
        sharedGroupCodes = DefensiveCopies.copyList(sharedGroupCodes);
    }

}
