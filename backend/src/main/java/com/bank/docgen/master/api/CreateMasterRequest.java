package com.bank.docgen.master.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMasterRequest(
        @NotBlank @Size(max = 64) String groupCode,
        @NotBlank @Size(max = 256) String name,
        @Size(max = 1024) String description
) {
}
