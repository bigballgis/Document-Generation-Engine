package com.bank.docgen.runtime.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
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
            throw modeNotAllowed();
        }
    }

    public static void validateBatchEndpoint(String mode, List<String> allowedModes, boolean syncEndpoint) {
        rejectBlank(mode);
        rejectSyncDownloadUrl(mode);
        requireAllowed(mode, allowedModes);
        if (syncEndpoint) {
            if (!"SYNC_STREAM".equalsIgnoreCase(mode)) {
                throw modeNotAllowed();
            }
        } else if (!"ASYNC_TASK".equalsIgnoreCase(mode)) {
            throw modeNotAllowed();
        }
    }

    private static void rejectBlank(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private static void rejectSyncDownloadUrl(String mode) {
        if ("SYNC_DOWNLOAD_URL".equalsIgnoreCase(mode)) {
            throw modeNotAllowed();
        }
    }

    private static void requireAllowed(String mode, List<String> allowedModes) {
        if (allowedModes.stream().noneMatch(item -> item.equalsIgnoreCase(mode))) {
            throw modeNotAllowed();
        }
    }

    /** FOS-W11-6: sync + batch share {@code OUTPUT_MODE_NOT_ALLOWED}. */
    private static RuntimeBatchValidationException modeNotAllowed() {
        return new RuntimeBatchValidationException(
                ApiErrorCodes.OUTPUT_MODE_NOT_ALLOWED,
                "api.error.runtime.outputModeUnsupported"
        );
    }
}
