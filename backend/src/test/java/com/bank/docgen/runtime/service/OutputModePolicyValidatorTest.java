package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutputModePolicyValidatorTest {

    @Test
    void validateSyncGenerate_rejectsAsyncTaskEvenWhenPolicyAllowsIt() {
        List<String> allowed = List.of("SYNC_STREAM", "ASYNC_TASK");

        assertThatThrownBy(() -> OutputModePolicyValidator.validateSyncGenerate("ASYNC_TASK", allowed))
                .isInstanceOf(RuntimeBatchValidationException.class)
                .satisfies(ex -> {
                    RuntimeBatchValidationException typed = (RuntimeBatchValidationException) ex;
                    assertThat(typed.errorCode()).isEqualTo(ApiErrorCodes.OUTPUT_MODE_NOT_ALLOWED);
                    assertThat(typed.messageKey()).isEqualTo("api.error.runtime.outputModeUnsupported");
                });
    }

    @Test
    void validateBatchEndpoint_rejectsModeMissingFromPolicy() {
        List<String> allowed = List.of("SYNC_STREAM");

        assertThatThrownBy(() -> OutputModePolicyValidator.validateBatchEndpoint("ASYNC_TASK", allowed, false))
                .isInstanceOf(RuntimeBatchValidationException.class)
                .satisfies(ex -> {
                    RuntimeBatchValidationException typed = (RuntimeBatchValidationException) ex;
                    assertThat(typed.errorCode()).isEqualTo(ApiErrorCodes.OUTPUT_MODE_NOT_ALLOWED);
                });
    }

    @Test
    void validateBatchEndpoint_rejectsSyncDownloadUrl() {
        assertThatThrownBy(() -> OutputModePolicyValidator.validateBatchEndpoint(
                "SYNC_DOWNLOAD_URL",
                List.of("SYNC_DOWNLOAD_URL", "SYNC_STREAM"),
                true
        ))
                .isInstanceOf(RuntimeBatchValidationException.class)
                .satisfies(ex -> {
                    RuntimeBatchValidationException typed = (RuntimeBatchValidationException) ex;
                    assertThat(typed.errorCode()).isEqualTo(ApiErrorCodes.OUTPUT_MODE_NOT_ALLOWED);
                });
    }
}
