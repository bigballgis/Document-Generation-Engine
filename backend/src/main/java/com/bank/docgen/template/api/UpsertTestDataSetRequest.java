package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpsertTestDataSetRequest(
        @NotBlank String name,
        String description,
        @NotNull Map<String, Object> variables
) {
}
