package com.bank.docgen.contentmodule.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateContentModuleVersionRequest(
        @NotBlank @Size(max = 32) String semanticVersion,
        @NotBlank String contentStructureJson,
        @Size(max = 2048) String changeDescription,
        @Size(max = 128) String jurisdiction,
        Instant effectiveFrom,
        Instant effectiveTo,
        @Size(max = 128) String legalReviewRef
) {
    public CreateContentModuleVersionRequest(
            String semanticVersion,
            String contentStructureJson,
            String changeDescription
    ) {
        this(semanticVersion, contentStructureJson, changeDescription, null, null, null, null);
    }
}
