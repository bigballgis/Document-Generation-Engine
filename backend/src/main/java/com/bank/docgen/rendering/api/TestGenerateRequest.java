package com.bank.docgen.rendering.api;

import java.util.Map;

public record TestGenerateRequest(
        Map<String, Object> variables,
        String testDataSetId
) {
}
