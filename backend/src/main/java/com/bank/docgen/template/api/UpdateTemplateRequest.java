package com.bank.docgen.template.api;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateTemplateRequest(
        @Size(max = 256) String name,
        @Size(max = 1024) String description,
        @Size(max = 64) String locale,
        UUID localeVariantFamilyId
) {
    public UpdateTemplateRequest(String name, String description) {
        this(name, description, null, null);
    }
}
