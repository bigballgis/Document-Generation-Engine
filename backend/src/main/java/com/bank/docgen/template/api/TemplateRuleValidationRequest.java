package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TemplateRuleValidationRequest(
        @NotEmpty List<@Valid TemplateRuleValidationItemRequest> rules
) {
    public TemplateRuleValidationRequest {
        rules = DefensiveCopies.copyList(rules);
    }
}
