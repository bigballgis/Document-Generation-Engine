package com.bank.docgen.runtime.api;

import java.util.List;

public record SyncGenerateResult(
        byte[] artifactBytes,
        String contentType,
        String documentId,
        String resolvedReleaseVersion,
        List<String> fidelityWarningCodes,
        String idempotencyStatus
) {
}