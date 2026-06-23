package com.bank.docgen.authorization.management.api;

import com.bank.docgen.authorization.management.domain.ManagementRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateUserRequest(
        @NotBlank
        @Pattern(regexp = "\\d{8}", message = "username must be an 8-digit employee id")
        String username,

        @NotBlank
        @Size(max = 128)
        String displayName,

        @NotBlank
        @Email
        @Size(max = 256)
        String email,

        @NotBlank
        @Size(min = 12, max = 128)
        String initialPassword,

        @NotNull
        List<ManagementRole> roles,

        @NotNull
        List<String> authorizedGroupCodes
) {
}
