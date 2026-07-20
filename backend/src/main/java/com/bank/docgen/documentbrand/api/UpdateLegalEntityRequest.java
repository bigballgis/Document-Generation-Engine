package com.bank.docgen.documentbrand.api;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLegalEntityRequest(
        @NotBlank String groupCode,
        @Size(max = 256) String displayName,
        DocumentBrandStatus status,
        @Size(max = 64) String documentBrandCode
) {
}
