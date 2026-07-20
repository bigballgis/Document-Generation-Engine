package com.bank.docgen.documentbrand.api;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLegalEntityRequest(
        @NotBlank String groupCode,
        @NotBlank @Size(max = 64) String legalEntityCode,
        @NotBlank @Size(max = 256) String displayName,
        DocumentBrandStatus status,
        @NotBlank @Size(max = 64) String documentBrandCode
) {
}
