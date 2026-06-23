package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotBlank;

public record LifecycleGovernanceRequest(
        @NotBlank String reason,
        boolean confirmed
) {
}
