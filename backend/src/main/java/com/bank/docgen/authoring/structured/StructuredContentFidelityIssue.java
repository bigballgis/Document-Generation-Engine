package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;

public record StructuredContentFidelityIssue(
        StructuredContentFidelitySeverity severity,
        FidelityWarningCode code,
        String messageKey,
        String location,
        String detectionSummary,
        String suggestion
) {
}
