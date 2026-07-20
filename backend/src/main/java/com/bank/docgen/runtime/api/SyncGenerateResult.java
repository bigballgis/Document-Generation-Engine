package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.io.InputStream;
import java.util.List;

public record SyncGenerateResult(
        byte[] artifactBytes,
        InputStream artifactStream,
        String contentType,
        String documentId,
        String resolvedReleaseVersion,
        List<String> fidelityWarningCodes,
        String idempotencyStatus,
        String resolvedLegalEntityCode,
        String resolvedDocumentBrandCode
) {
    public SyncGenerateResult {
        artifactBytes = DefensiveCopies.copyBytes(artifactBytes);
        fidelityWarningCodes = DefensiveCopies.copyList(fidelityWarningCodes);
    }

    /** Compatibility constructor for callers that omit IBL-E4 resolved brand codes. */
    public SyncGenerateResult(
            byte[] artifactBytes,
            InputStream artifactStream,
            String contentType,
            String documentId,
            String resolvedReleaseVersion,
            List<String> fidelityWarningCodes,
            String idempotencyStatus
    ) {
        this(
                artifactBytes,
                artifactStream,
                contentType,
                documentId,
                resolvedReleaseVersion,
                fidelityWarningCodes,
                idempotencyStatus,
                null,
                null
        );
    }
}
