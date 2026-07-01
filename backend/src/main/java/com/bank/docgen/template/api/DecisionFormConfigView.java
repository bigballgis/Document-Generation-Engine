package com.bank.docgen.template.api;

import java.util.List;
import java.util.Map;

public record DecisionFormConfigView(
        List<String> reasonCategories,
        Map<String, String> riskPromptCopy
) {
}
