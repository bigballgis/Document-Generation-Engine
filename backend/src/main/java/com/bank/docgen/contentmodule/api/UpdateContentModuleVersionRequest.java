package com.bank.docgen.contentmodule.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateContentModuleVersionRequest(
        @NotBlank String contentStructureJson,
        String changeDescription
) {
}
