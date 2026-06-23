package com.bank.docgen.authorization.management.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @NotBlank
        @Size(max = 128)
        String displayName
) {
}
