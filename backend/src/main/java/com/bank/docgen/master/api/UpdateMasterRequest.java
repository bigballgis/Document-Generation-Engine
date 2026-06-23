package com.bank.docgen.master.api;

import jakarta.validation.constraints.Size;

public record UpdateMasterRequest(
        @Size(max = 256) String name,
        @Size(max = 1024) String description
) {
}
