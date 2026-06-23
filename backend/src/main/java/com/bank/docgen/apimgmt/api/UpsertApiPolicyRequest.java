package com.bank.docgen.apimgmt.api;

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
}
