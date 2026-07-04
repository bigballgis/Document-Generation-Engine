package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    public ApiPolicySummaryView {
        allowedOutputFormats = DefensiveCopies.copyStringList(allowedOutputFormats);
        allowedOutputModes = DefensiveCopies.copyStringList(allowedOutputModes);
    }
}
