package com.bank.docgen.contentmodule.api;

import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContentModuleReviewTransitionRequest(
        @NotNull ContentModuleReviewOperation operation,
        @NotNull ContentModuleGovernanceActorRole actorRole,
        @NotBlank String actorId,
        String changeDescription,
        String rejectionReason
) {
}
