package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.template.service.TemplateValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutputModePolicyValidatorTest {

    @Test
    void validateSyncGenerate_rejectsAsyncTaskEvenWhenPolicyAllowsIt() {
        List<String> allowed = List.of("SYNC_STREAM", "ASYNC_TASK");

        assertThatThrownBy(() -> OutputModePolicyValidator.validateSyncGenerate("ASYNC_TASK", allowed))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.runtime.outputModeUnsupported");
    }

    @Test
    void validateBatchEndpoint_rejectsModeMissingFromPolicy() {
        List<String> allowed = List.of("SYNC_STREAM");

        assertThatThrownBy(() -> OutputModePolicyValidator.validateBatchEndpoint("ASYNC_TASK", allowed, false))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.runtime.outputModeUnsupported");
    }

    @Test
    void validateBatchEndpoint_rejectsSyncDownloadUrl() {
        assertThatThrownBy(() -> OutputModePolicyValidator.validateBatchEndpoint(
                "SYNC_DOWNLOAD_URL",
                List.of("SYNC_DOWNLOAD_URL", "SYNC_STREAM"),
                true
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.runtime.outputModeUnsupported");
    }
}
