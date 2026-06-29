package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotBlank;

public record UpsertContentModuleReferenceRequest(
        @NotBlank String referenceKey,
        @NotBlank String moduleId,
        @NotBlank String semanticVersion
) {
}
