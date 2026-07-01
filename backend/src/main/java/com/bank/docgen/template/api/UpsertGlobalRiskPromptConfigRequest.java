package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record UpsertGlobalRiskPromptConfigRequest(
        @NotEmpty List<@Size(max = 64) String> reasonCategories,
        @NotEmpty Map<@Size(max = 64) String, @Size(max = 2048) String> riskPromptCopy
) {
}
