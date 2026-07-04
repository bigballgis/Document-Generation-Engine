package com.bank.docgen.apimgmt.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record UpsertApiPolicyRequest(
        List<String> allowedAdGroups,
        String defaultRouteReleaseVersion,
        List<String> outputFormats,
        List<String> outputModes,
        boolean batchEnabled,
        int maxBatchSize,
        boolean docxEncryptionEnabled,
        boolean pdfEncryptionEnabled
) {
    public UpsertApiPolicyRequest {
        allowedAdGroups = DefensiveCopies.copyList(allowedAdGroups);
        outputFormats = DefensiveCopies.copyList(outputFormats);
        outputModes = DefensiveCopies.copyList(outputModes);
    }

}
