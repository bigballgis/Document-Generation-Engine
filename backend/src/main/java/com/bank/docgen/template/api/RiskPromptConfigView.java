package com.bank.docgen.template.api;

import java.util.List;
import java.util.Map;

public record RiskPromptConfigView(
        String scopeType,
        String groupCode,
        List<String> reasonCategories,
        Map<String, String> riskPromptCopy,
        String updatedAt
) {
}
