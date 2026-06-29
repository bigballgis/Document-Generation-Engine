package com.bank.docgen.contentmodule.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateContentModuleRequest(
        @NotBlank @Size(max = 128) String moduleCode,
        @NotBlank @Size(max = 64) String groupCode,
        @NotBlank @Size(max = 256) String name,
        @Size(max = 1024) String description,
        List<@NotBlank @Size(max = 64) String> sharedGroupCodes,
        @NotBlank @Size(max = 32) String semanticVersion,
        @NotBlank String contentStructureJson,
        @Size(max = 2048) String changeDescription
) {
}
