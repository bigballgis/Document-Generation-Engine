package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TemplateSummaryView(
        String id,
        String externalId,
        String groupCode,
        String name,
        TemplateLifecycleStatus lifecycleStatus,
        ApprovalSubState approvalSubState,
        String releaseVersion,
        int releaseVersionCount,
        String masterId,
        String updatedBy,
        Instant updatedAt,
        String updatedByDisplayName,
        LocalDate nextReviewDue,
        String locale,
        String localeVariantFamilyId
) {
    /** Compatibility constructor for callers that omit locale fields. */
    public TemplateSummaryView(
            String id,
            String externalId,
            String groupCode,
            String name,
            TemplateLifecycleStatus lifecycleStatus,
            ApprovalSubState approvalSubState,
            String releaseVersion,
            int releaseVersionCount,
            String masterId,
            String updatedBy,
            Instant updatedAt,
            String updatedByDisplayName,
            LocalDate nextReviewDue
    ) {
        this(
                id,
                externalId,
                groupCode,
                name,
                lifecycleStatus,
                approvalSubState,
                releaseVersion,
                releaseVersionCount,
                masterId,
                updatedBy,
                updatedAt,
                updatedByDisplayName,
                nextReviewDue,
                ComputeDslLimits.DEFAULT_LOCALE,
                null
        );
    }
}
