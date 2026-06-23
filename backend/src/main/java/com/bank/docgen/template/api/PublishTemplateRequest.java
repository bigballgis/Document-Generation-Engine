package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PublishTemplateRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+$")
        String releaseVersion
) {
}
