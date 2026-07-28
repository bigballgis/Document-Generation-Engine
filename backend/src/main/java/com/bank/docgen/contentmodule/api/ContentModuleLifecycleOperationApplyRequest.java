package com.bank.docgen.contentmodule.api;

import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContentModuleLifecycleOperationApplyRequest(
        @NotNull ContentModuleLifecycleOperation operationType,
        @NotNull ContentModuleGovernanceActorRole actorRole,
        @NotBlank String actorId,
        @NotNull Boolean impactSummaryViewed,
        @NotNull Boolean secondConfirmation,
        ContentModuleLifecycleImpactSummaryView impactSummary,
        UUID versionId,
        String semanticVersion
) {
    public ContentModuleLifecycleOperationApplyRequest(
            ContentModuleLifecycleOperation operationType,
            ContentModuleGovernanceActorRole actorRole,
            String actorId,
            Boolean impactSummaryViewed,
            Boolean secondConfirmation,
            ContentModuleLifecycleImpactSummaryView impactSummary
    ) {
        this(
                operationType,
                actorRole,
                actorId,
                impactSummaryViewed,
                secondConfirmation,
                impactSummary,
                null,
                null
        );
    }
}
