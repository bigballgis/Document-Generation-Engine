package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.template.domain.ApprovalMatrixMode;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateTemplateRequest(
        @Size(max = 256) String name,
        @Size(max = 1024) String description,
        @Size(max = 64) String locale,
        UUID localeVariantFamilyId,
        ApprovalMatrixMode approvalMatrixMode,
        List<String> allowedDocumentBrandCodes
) {
    public UpdateTemplateRequest {
        allowedDocumentBrandCodes = DefensiveCopies.copyList(allowedDocumentBrandCodes);
    }

    public UpdateTemplateRequest(String name, String description) {
        this(name, description, null, null, null, null);
    }

    /** Compatibility constructor — locale/family without matrix mode. */
    public UpdateTemplateRequest(
            String name,
            String description,
            String locale,
            UUID localeVariantFamilyId
    ) {
        this(name, description, locale, localeVariantFamilyId, null, null);
    }

    /** Compatibility constructor — matrix mode without allow-list. */
    public UpdateTemplateRequest(
            String name,
            String description,
            String locale,
            UUID localeVariantFamilyId,
            ApprovalMatrixMode approvalMatrixMode
    ) {
        this(name, description, locale, localeVariantFamilyId, approvalMatrixMode, null);
    }
}
