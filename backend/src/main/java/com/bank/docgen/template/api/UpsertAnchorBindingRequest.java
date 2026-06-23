package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.AnchorContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertAnchorBindingRequest(
        @NotBlank String anchorId,
        @NotNull AnchorContentType declaredContentType,
        @NotBlank String structuredContentJson
) {
}
