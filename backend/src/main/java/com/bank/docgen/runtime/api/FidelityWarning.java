package com.bank.docgen.runtime.api;

/**
 * Runtime API fidelity warning object matching OpenAPI {@code FidelityWarning}.
 *
 * <p>Distinct from management preview {@code rendering.api.FidelityWarningView}
 * ({@code code}/{@code location}/{@code artifact}/{@code viewed}).
 */
public record FidelityWarning(
        String warningCode,
        String messageKey,
        String message,
        String locationSummary,
        String detectedSummary,
        String recommendation,
        boolean sensitiveDataExcluded
) {
}
