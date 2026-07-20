package com.bank.docgen.apimgmt.api;

import java.util.UUID;

/**
 * Safe audit summary for {@code INVOCATION_REGENERATED} (no variables).
 * Template/group scope enables management audit filtering (CE-G06 arch suggestion).
 * PD-6 adds {@code productionReissue}/{@code specimen}/{@code reason}.
 */
public record InvocationRegeneratedAuditDetail(
        String sourceInvocationId,
        String regenerationId,
        String releaseBundleSnapshotId,
        String releaseBundleHash,
        String outputFormat,
        String outcome,
        String errorCode,
        String actorUsername,
        boolean encryptionReapplied,
        UUID templateId,
        String groupCode,
        boolean productionReissue,
        boolean specimen,
        String reason
) {
}
