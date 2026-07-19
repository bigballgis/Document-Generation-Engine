package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;
import com.bank.docgen.template.domain.ApprovalMatrixMode;
import com.bank.docgen.template.domain.ApprovalStage;
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
        String localeVariantFamilyId,
        ApprovalMatrixMode approvalMatrixMode,
        ApprovalStage approvalStage
) {
    public TemplateSummaryView {
        if (approvalMatrixMode == null) {
            approvalMatrixMode = ApprovalMatrixMode.SINGLE_TRACK;
        }
    }

    /** Compatibility constructor for callers that omit locale + matrix fields. */
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
                null,
                ApprovalMatrixMode.SINGLE_TRACK,
                null
        );
    }

    /** Compatibility constructor for callers that omit matrix fields. */
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
            LocalDate nextReviewDue,
            String locale,
            String localeVariantFamilyId
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
                locale,
                localeVariantFamilyId,
                ApprovalMatrixMode.SINGLE_TRACK,
                null
        );
    }
}
