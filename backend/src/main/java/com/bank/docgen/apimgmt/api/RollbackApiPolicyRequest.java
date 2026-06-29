package com.bank.docgen.apimgmt.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RollbackApiPolicyRequest(
        @NotNull @Min(1) Integer policyVersion,
        boolean confirmed
) {
}
