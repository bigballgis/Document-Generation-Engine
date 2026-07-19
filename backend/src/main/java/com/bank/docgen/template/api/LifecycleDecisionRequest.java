package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.ApprovalStage;
import com.bank.docgen.template.domain.LifecycleDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LifecycleDecisionRequest(
        @NotNull LifecycleDecision decision,
        String commentSummary,
        @Size(max = 64) String reasonCategory,
        @Size(max = 2048) String impactSummary,
        Boolean fidelityViewedConfirmed,
        Boolean coverageViewedConfirmed,
        Boolean previewViewedConfirmed,
        Boolean keyEvidenceConfirmed,
        @Size(max = 64) String remediationTestRecordId,
        @Size(max = 64) String remediationChangeDiffRef,
        @Size(max = 64) String remediationChecklistCode,
        Boolean exceptionIntervention,
        @Size(max = 2048) String exceptionReason,
        Boolean secondaryConfirmed,
        ApprovalStage approvalStage
) {
    public LifecycleDecisionRequest(
            LifecycleDecision decision,
            String commentSummary,
            String reasonCategory,
            String impactSummary
    ) {
        this(
                decision,
                commentSummary,
                reasonCategory,
                impactSummary,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /** Compatibility constructor — full CE-G01 fields without approvalStage. */
    public LifecycleDecisionRequest(
            LifecycleDecision decision,
            String commentSummary,
            String reasonCategory,
            String impactSummary,
            Boolean fidelityViewedConfirmed,
            Boolean coverageViewedConfirmed,
            Boolean previewViewedConfirmed,
            Boolean keyEvidenceConfirmed,
            String remediationTestRecordId,
            String remediationChangeDiffRef,
            String remediationChecklistCode,
            Boolean exceptionIntervention,
            String exceptionReason,
            Boolean secondaryConfirmed
    ) {
        this(
                decision,
                commentSummary,
                reasonCategory,
                impactSummary,
                fidelityViewedConfirmed,
                coverageViewedConfirmed,
                previewViewedConfirmed,
                keyEvidenceConfirmed,
                remediationTestRecordId,
                remediationChangeDiffRef,
                remediationChecklistCode,
                exceptionIntervention,
                exceptionReason,
                secondaryConfirmed,
                null
        );
    }
}
