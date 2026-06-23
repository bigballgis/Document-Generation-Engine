package com.bank.docgen.runtime.api;

import java.time.Instant;
import java.util.List;

public record ApiPolicySummaryView(
        int policyVersion,
        Instant updatedAt,
        String updatedBy,
        List<String> allowedOutputFormats,
        List<String> allowedOutputModes,
        BatchLimitsView batchLimits,
        EncryptionCapabilitiesView encryptionCapabilities,
        AdGroupAuthorizationSummaryView adGroupAuthorizationSummary,
        RuntimeCredentialSummaryView credentialSummary
) {
}
