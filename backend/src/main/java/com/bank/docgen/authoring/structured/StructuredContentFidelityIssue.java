package com.bank.docgen.authoring.structured;

import com.bank.docgen.rendering.domain.FidelityWarningCode;

public record StructuredContentFidelityIssue(
        StructuredContentFidelitySeverity severity,
        FidelityWarningCode code,
        String messageKey,
        String location,
        String detectionSummary,
        String suggestion
) {
}
