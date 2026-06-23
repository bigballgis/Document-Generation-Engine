package com.bank.docgen.template.api;

import jakarta.validation.constraints.Size;

public record UpdateTemplateRequest(
        @Size(max = 256) String name,
        @Size(max = 1024) String description
) {
}
