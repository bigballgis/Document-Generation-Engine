package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * IBL-E5 / ADR-0066 bulk re-pin request. {@code dryRun} is required.
 */
public record BulkRepinContentModuleReferencesRequest(
        String groupCode,
        @NotBlank String contentModuleId,
        String fromSemanticVersion,
        String toSemanticVersion,
        Boolean useLatestApproved,
        List<String> templateIds,
        @NotNull Boolean dryRun
) {
}
