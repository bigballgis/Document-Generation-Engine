package com.bank.docgen.apimgmt.api;

import java.util.UUID;

/**
 * CE-G06 regenerate success result — never includes variables or encryption passwords
 * (HIST C6 / ADR-0057).
 */
public record ManagementInvocationRegenerateView(
        UUID regenerationId,
        String sourceInvocationId,
        UUID releaseBundleSnapshotId,
        String releaseBundleHash,
        String outputFormat,
        boolean specimen,
        boolean encryptionReapplied,
        String downloadUrl,
        String artifactPath
) {
}
