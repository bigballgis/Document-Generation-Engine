package com.bank.docgen.authorization.management.api;

import com.bank.docgen.authorization.management.domain.ManagementRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateUserRequest(
        @NotBlank
        @Size(max = 128)
        String displayName,

        @NotBlank
        @Email
        @Size(max = 256)
        String email,

        @NotNull
        List<ManagementRole> roles,

        @NotNull
        List<String> authorizedGroupCodes
) {
    public UpdateUserRequest {
        roles = copyRoles(roles);
        authorizedGroupCodes = copyStrings(authorizedGroupCodes);
    }

    private static List<ManagementRole> copyRoles(List<ManagementRole> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<String> copyStrings(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
