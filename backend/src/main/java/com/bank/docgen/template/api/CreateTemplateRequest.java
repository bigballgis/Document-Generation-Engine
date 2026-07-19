package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;
import com.bank.docgen.template.domain.ApprovalMatrixMode;
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
        UUID localeVariantFamilyId,
        ApprovalMatrixMode approvalMatrixMode
) {
    /** Compatibility constructor — defaults body locale to compute default for legacy callers. */
    public CreateTemplateRequest(
            String externalId,
            String groupCode,
            String name,
            String description,
            String masterId
    ) {
        this(externalId, groupCode, name, description, masterId, ComputeDslLimits.DEFAULT_LOCALE, null, null);
    }

    /** Compatibility constructor — locale/family without matrix mode (defaults SINGLE_TRACK). */
    public CreateTemplateRequest(
            String externalId,
            String groupCode,
            String name,
            String description,
            String masterId,
            String locale,
            UUID localeVariantFamilyId
    ) {
        this(externalId, groupCode, name, description, masterId, locale, localeVariantFamilyId, null);
    }
}
