package com.bank.docgen.runtime.service;

import com.bank.docgen.template.service.TemplateValidationException;
import java.util.List;

/**
 * Shared output.mode validation for sync generate and batch endpoints.
 */
public final class OutputModePolicyValidator {

    private OutputModePolicyValidator() {
    }

    public static void validateSyncGenerate(String mode, List<String> allowedModes) {
        rejectBlank(mode);
        rejectSyncDownloadUrl(mode);
        requireAllowed(mode, allowedModes);
        if (!"SYNC_STREAM".equalsIgnoreCase(mode)) {
            throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
        }
    }

    public static void validateBatchEndpoint(String mode, List<String> allowedModes, boolean syncEndpoint) {
        rejectBlank(mode);
        rejectSyncDownloadUrl(mode);
        requireAllowed(mode, allowedModes);
        if (syncEndpoint) {
            if (!"SYNC_STREAM".equalsIgnoreCase(mode)) {
                throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
            }
        } else if (!"ASYNC_TASK".equalsIgnoreCase(mode)) {
            throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
        }
    }

    private static void rejectBlank(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private static void rejectSyncDownloadUrl(String mode) {
        if ("SYNC_DOWNLOAD_URL".equalsIgnoreCase(mode)) {
            throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
        }
    }

    private static void requireAllowed(String mode, List<String> allowedModes) {
        if (allowedModes.stream().noneMatch(item -> item.equalsIgnoreCase(mode))) {
            throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
        }
    }
}
