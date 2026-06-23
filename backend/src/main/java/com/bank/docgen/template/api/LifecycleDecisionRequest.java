package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.LifecycleDecision;
import jakarta.validation.constraints.NotNull;

public record LifecycleDecisionRequest(
        @NotNull LifecycleDecision decision,
        String commentSummary
) {
}
