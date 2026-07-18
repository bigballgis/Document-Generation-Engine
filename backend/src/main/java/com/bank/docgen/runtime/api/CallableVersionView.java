package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CallableVersionView(
        String releaseVersion,
        String explicitVersionUrl,
        Boolean deprecated,
        Instant sunsetAt,
        List<ContractVariableSchemaView> variables
) {
    public CallableVersionView {
        if (variables != null) {
            variables = DefensiveCopies.copyList(variables);
        }
    }

    public CallableVersionView(String releaseVersion, String explicitVersionUrl) {
        this(releaseVersion, explicitVersionUrl, Boolean.FALSE, null, null);
    }

    public CallableVersionView(
            String releaseVersion,
            String explicitVersionUrl,
            Boolean deprecated,
            Instant sunsetAt
    ) {
        this(releaseVersion, explicitVersionUrl, deprecated, sunsetAt, null);
    }
}
