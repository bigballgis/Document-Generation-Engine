package com.bank.docgen.contentmodule.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContentModuleVersionRequest(
        @NotBlank @Size(max = 32) String semanticVersion,
        @NotBlank String contentStructureJson,
        @Size(max = 2048) String changeDescription
) {
}
