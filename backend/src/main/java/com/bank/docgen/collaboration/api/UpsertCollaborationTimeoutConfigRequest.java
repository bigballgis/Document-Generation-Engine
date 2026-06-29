package com.bank.docgen.collaboration.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertCollaborationTimeoutConfigRequest(
        @NotNull String scopeType,
        @Size(max = 64) String groupCode,
        @NotNull @Min(1) @Max(8760) Integer testThresholdHours,
        @NotNull @Min(1) @Max(8760) Integer approvalThresholdHours,
        @NotNull @Min(1) @Max(8760) Integer pendingReleaseThresholdHours,
        @NotNull @Min(1) @Max(8760) Integer remediationThresholdHours
) {
}
