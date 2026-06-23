package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotBlank;

public record CreateTemplateRequest(
        @NotBlank String externalId,
        @NotBlank String groupCode,
        @NotBlank String name,
        String description,
        @NotBlank String masterId
) {
}
