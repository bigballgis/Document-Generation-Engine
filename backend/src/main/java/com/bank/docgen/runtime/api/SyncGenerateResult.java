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
        String idempotencyStatus
) {
    public SyncGenerateResult {
        fidelityWarningCodes = DefensiveCopies.copyList(fidelityWarningCodes);
    }

}
