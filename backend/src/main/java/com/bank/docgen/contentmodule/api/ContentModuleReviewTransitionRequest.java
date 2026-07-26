package com.bank.docgen.contentmodule.api;

import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ContentModuleReviewTransitionRequest(
        @NotNull ContentModuleReviewOperation operation,
        @NotNull ContentModuleGovernanceActorRole actorRole,
        @NotBlank String actorId,
        String changeDescription,
        String rejectionReason,
        Boolean exceptionIntervention,
        @Size(max = 2048) String exceptionReason,
        Boolean secondaryConfirmed,
        UUID versionId,
        String semanticVersion
) {
    public ContentModuleReviewTransitionRequest(
            ContentModuleReviewOperation operation,
            ContentModuleGovernanceActorRole actorRole,
            String actorId,
            String changeDescription,
            String rejectionReason
    ) {
        this(operation, actorRole, actorId, changeDescription, rejectionReason, null, null, null, null, null);
    }

    public ContentModuleReviewTransitionRequest(
            ContentModuleReviewOperation operation,
            ContentModuleGovernanceActorRole actorRole,
            String actorId,
            String changeDescription,
            String rejectionReason,
            Boolean exceptionIntervention,
            String exceptionReason,
            Boolean secondaryConfirmed
    ) {
        this(
                operation,
                actorRole,
                actorId,
                changeDescription,
                rejectionReason,
                exceptionIntervention,
                exceptionReason,
                secondaryConfirmed,
                null,
                null
        );
    }

    public ContentModuleReviewTransitionRequest(
            ContentModuleReviewOperation operation,
            ContentModuleGovernanceActorRole actorRole,
            String actorId,
            String changeDescription,
            String rejectionReason,
            UUID versionId,
            String semanticVersion
    ) {
        this(
                operation,
                actorRole,
                actorId,
                changeDescription,
                rejectionReason,
                null,
                null,
                null,
                versionId,
                semanticVersion
        );
    }
}
