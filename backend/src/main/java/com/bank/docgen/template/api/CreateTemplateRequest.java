package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateTemplateRequest(
        @NotBlank String externalId,
        @NotBlank String groupCode,
        @NotBlank String name,
        String description,
        @NotBlank String masterId,
        @NotBlank @Size(max = 64) String locale,
        UUID localeVariantFamilyId
) {
    /** Compatibility constructor — defaults body locale to compute default for legacy callers. */
    public CreateTemplateRequest(
            String externalId,
            String groupCode,
            String name,
            String description,
            String masterId
    ) {
        this(externalId, groupCode, name, description, masterId, ComputeDslLimits.DEFAULT_LOCALE, null);
    }
}
