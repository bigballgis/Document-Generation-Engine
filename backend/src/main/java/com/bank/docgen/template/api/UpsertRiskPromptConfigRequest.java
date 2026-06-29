package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record UpsertRiskPromptConfigRequest(
        @NotNull String scopeType,
        @Size(max = 64) String groupCode,
        @NotEmpty List<String> reasonCategories,
        @NotEmpty Map<String, String> riskPromptCopy
) {
}
