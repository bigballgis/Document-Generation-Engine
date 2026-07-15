package com.bank.docgen.contentmodule.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateContentModuleVersionRequest(
        @NotBlank String contentStructureJson,
        String changeDescription,
        @Size(max = 128) String jurisdiction,
        Instant effectiveFrom,
        Instant effectiveTo,
        @Size(max = 128) String legalReviewRef
) {
    public UpdateContentModuleVersionRequest(String contentStructureJson, String changeDescription) {
        this(contentStructureJson, changeDescription, null, null, null, null);
    }
}
