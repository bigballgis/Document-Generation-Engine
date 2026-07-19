package com.bank.docgen.contentmodule.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;

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
        Instant updatedAt,
        String locale,
        String localeVariantFamilyId
) {
    public ContentModuleSummaryView {
        sharedGroupCodes = DefensiveCopies.copyList(sharedGroupCodes);
    }

    /** Compatibility constructor for callers that omit locale fields. */
    public ContentModuleSummaryView(
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
        this(
                moduleId,
                moduleCode,
                groupCode,
                name,
                description,
                sharedGroupCodes,
                reviewState,
                lifecycleState,
                createdAt,
                updatedAt,
                ComputeDslLimits.DEFAULT_LOCALE,
                null
        );
    }
}
