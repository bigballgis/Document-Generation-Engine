package com.bank.docgen.template.api;

import java.util.List;
import java.util.Map;

public record TemplateRiskPromptConfigView(
        boolean useDefault,
        List<String> reasonCategories,
        Map<String, String> riskPromptCopy,
        String updatedAt
) {
}
