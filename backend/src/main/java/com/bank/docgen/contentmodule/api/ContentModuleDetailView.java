package com.bank.docgen.contentmodule.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;

import java.util.List;

public record ContentModuleDetailView(
        String moduleId,
        String moduleCode,
        String groupCode,
        String name,
        String description,
        List<String> sharedGroupCodes,
        List<ContentModuleVersionView> versions,
        List<ContentModuleReviewRecordView> reviewHistory,
        String locale,
        String localeVariantFamilyId
) {
    public ContentModuleDetailView {
        sharedGroupCodes = DefensiveCopies.copyStringList(sharedGroupCodes);
        versions = DefensiveCopies.copyList(versions);
        reviewHistory = DefensiveCopies.copyList(reviewHistory);
    }

    /** Compatibility constructor for callers that omit locale fields. */
    public ContentModuleDetailView(
            String moduleId,
            String moduleCode,
            String groupCode,
            String name,
            String description,
            List<String> sharedGroupCodes,
            List<ContentModuleVersionView> versions,
            List<ContentModuleReviewRecordView> reviewHistory
    ) {
        this(
                moduleId,
                moduleCode,
                groupCode,
                name,
                description,
                sharedGroupCodes,
                versions,
                reviewHistory,
                ComputeDslLimits.DEFAULT_LOCALE,
                null
        );
    }
}
