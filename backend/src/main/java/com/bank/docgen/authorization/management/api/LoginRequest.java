package com.bank.docgen.authorization.management.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Pattern(regexp = "\\d{8}")
        String username,
        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {
}
