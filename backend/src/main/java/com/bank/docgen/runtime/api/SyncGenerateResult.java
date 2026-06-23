package com.bank.docgen.runtime.api;

import java.io.InputStream;
import java.util.List;

public record SyncGenerateResult(
        byte[] artifactBytes,
        InputStream artifactStream,
        String contentType,
        String documentId,
        String resolvedReleaseVersion,
        List<String> fidelityWarningCodes,
        String idempotencyStatus
) {
}
