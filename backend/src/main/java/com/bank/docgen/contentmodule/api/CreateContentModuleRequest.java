package com.bank.docgen.contentmodule.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateContentModuleRequest(
        @NotBlank @Size(max = 128) String moduleCode,
        @NotBlank @Size(max = 64) String groupCode,
        @NotBlank @Size(max = 256) String name,
        @Size(max = 1024) String description,
        List<@NotBlank @Size(max = 64) String> sharedGroupCodes,
        @NotBlank @Size(max = 32) String semanticVersion,
        @NotBlank String contentStructureJson,
        @Size(max = 2048) String changeDescription,
        @NotBlank @Size(max = 64) String locale,
        UUID localeVariantFamilyId
) {
    public CreateContentModuleRequest {
        sharedGroupCodes = DefensiveCopies.copyList(sharedGroupCodes);
    }

    /** Compatibility constructor — defaults body locale for legacy callers. */
    public CreateContentModuleRequest(
            String moduleCode,
            String groupCode,
            String name,
            String description,
            List<String> sharedGroupCodes,
            String semanticVersion,
            String contentStructureJson,
            String changeDescription
    ) {
        this(
                moduleCode,
                groupCode,
                name,
                description,
                sharedGroupCodes,
                semanticVersion,
                contentStructureJson,
                changeDescription,
                ComputeDslLimits.DEFAULT_LOCALE,
                null
        );
    }
}
