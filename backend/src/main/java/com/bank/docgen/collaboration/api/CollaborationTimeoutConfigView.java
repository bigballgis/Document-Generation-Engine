package com.bank.docgen.collaboration.api;

public record CollaborationTimeoutConfigView(
        String scopeType,
        String groupCode,
        int testThresholdHours,
        int approvalThresholdHours,
        int pendingReleaseThresholdHours,
        int remediationThresholdHours,
        String updatedAt
) {
}
