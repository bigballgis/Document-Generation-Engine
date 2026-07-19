package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.ApprovalMatrixMode;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateTemplateRequest(
        @Size(max = 256) String name,
        @Size(max = 1024) String description,
        @Size(max = 64) String locale,
        UUID localeVariantFamilyId,
        ApprovalMatrixMode approvalMatrixMode
) {
    public UpdateTemplateRequest(String name, String description) {
        this(name, description, null, null, null);
    }

    /** Compatibility constructor — locale/family without matrix mode. */
    public UpdateTemplateRequest(
            String name,
            String description,
            String locale,
            UUID localeVariantFamilyId
    ) {
        this(name, description, locale, localeVariantFamilyId, null);
    }
}
