package com.bank.docgen.authorization.management.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Size(min = 12, max = 128)
        String newPassword
) {
}
