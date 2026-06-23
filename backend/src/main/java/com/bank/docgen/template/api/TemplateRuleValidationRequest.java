package com.bank.docgen.template.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TemplateRuleValidationRequest(
        @NotEmpty List<@Valid TemplateRuleValidationItemRequest> rules
) {
}
