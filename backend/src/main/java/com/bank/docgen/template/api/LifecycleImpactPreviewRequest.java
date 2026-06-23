package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.LifecycleGovernanceAction;
import jakarta.validation.constraints.NotNull;

public record LifecycleImpactPreviewRequest(
        @NotNull LifecycleGovernanceAction action,
        String releaseVersion
) {
}
