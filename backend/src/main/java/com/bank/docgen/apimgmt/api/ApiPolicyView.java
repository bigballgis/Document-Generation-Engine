package com.bank.docgen.apimgmt.api;

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
        boolean docxEncryptionEnabled,
        boolean pdfEncryptionEnabled,
        Instant updatedAt
) {
}
