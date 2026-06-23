package com.bank.docgen.authorization.management.api;

import com.bank.docgen.authorization.management.domain.GroupDimension;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "[A-Z0-9_]+", message = "groupCode must be uppercase letters, digits or underscores")
        String groupCode,

        @NotBlank
        @Size(max = 128)
        String displayName,

        @NotNull
        GroupDimension dimension
) {
}
