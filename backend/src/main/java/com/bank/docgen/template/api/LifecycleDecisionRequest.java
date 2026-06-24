package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.LifecycleDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LifecycleDecisionRequest(
        @NotNull LifecycleDecision decision,
        String commentSummary,
        @Size(max = 64) String reasonCategory,
        @Size(max = 2048) String impactSummary
) {
}
