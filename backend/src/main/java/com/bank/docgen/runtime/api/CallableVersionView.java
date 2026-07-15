package com.bank.docgen.runtime.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CallableVersionView(
        String releaseVersion,
        String explicitVersionUrl,
        Boolean deprecated,
        Instant sunsetAt
) {
    public CallableVersionView(String releaseVersion, String explicitVersionUrl) {
        this(releaseVersion, explicitVersionUrl, Boolean.FALSE, null);
    }
}
