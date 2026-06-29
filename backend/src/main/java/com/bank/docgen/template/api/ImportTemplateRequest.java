package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportTemplateRequest(
        @NotBlank String masterId,
        @NotNull @Valid TemplateExportBundleView bundle,
        TemplateImportConflictPolicy importConflictPolicy
) {
}
