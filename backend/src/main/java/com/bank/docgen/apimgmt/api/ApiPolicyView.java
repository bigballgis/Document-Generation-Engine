package com.bank.docgen.apimgmt.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.time.Instant;
import java.util.List;

public record ApiPolicyView(
        String templateId,
        int policyVersion,
        List<String> allowedAdGroups,
        String defaultRouteReleaseVersion,
        List<String> outputFormats,
        List<String> outputModes,
        boolean batchEnabled,
        int maxBatchSize,
        int batchSyncMaxItems,
        int batchAsyncMaxItems,
        boolean docxEncryptionEnabled,
        boolean pdfEncryptionEnabled,
        boolean saveGeneratedDocuments,
        int invocationRecordRetentionDays,
        int documentRetentionDays,
        Instant updatedAt
) {
    public ApiPolicyView {
        allowedAdGroups = DefensiveCopies.copyList(allowedAdGroups);
        outputFormats = DefensiveCopies.copyList(outputFormats);
        outputModes = DefensiveCopies.copyList(outputModes);
    }

}
