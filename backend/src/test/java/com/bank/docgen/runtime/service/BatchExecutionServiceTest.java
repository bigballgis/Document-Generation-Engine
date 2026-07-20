package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.sharedkernel.document.compute.VariableComputeException;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
import com.bank.docgen.sharedkernel.document.variable.VariableValidationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchExecutionServiceTest {

    @Mock
    private DocumentGenerationEngine documentGenerationEngine;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private MessageResolver messageResolver;

    private BatchExecutionService service;
    private TemplateEntity template;
    private BatchGenerateRequestBody request;

    @BeforeEach
    void setUp() {
        service = new BatchExecutionService(
                documentGenerationEngine,
                idempotencyService,
                messageResolver,
                new RuntimeFidelityWarningMapper(messageResolver)
        );
        template = new TemplateEntity(
                UUID.randomUUID(),
                "TPL-1",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        request = new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                List.of(
                        new BatchGenerateRequestBody.BatchGenerateItemBody(
                                "item-1",
                                Map.of("customerName", "Alice"),
                                null,
                                null
                        ),
                        new BatchGenerateRequestBody.BatchGenerateItemBody(
                                "item-2",
                                Map.of("customerName", "Bob"),
                                null,
                                null
                        )
                ),
                new EncryptionOptionsView(false, null, null, List.of()),
                "req-1",
                "idem-1",
                null,
                null
        );
    }

    @Test
    void bddCeC05_003_echoesOriginalBatchIdOnSuccessfulBatchResult() {
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(generated("DOC-1"))
                .thenReturn(generated("DOC-2"));

        BatchGenerateRequestBody withLineage = new BatchGenerateRequestBody(
                request.output(),
                request.items(),
                request.encryption(),
                request.requestId(),
                request.idempotencyKey(),
                "BATCH-ORIG02",
                request.context()
        );

        BatchExecutionService.BatchExecutionOutcome outcome = service.execute(
                template,
                "1.0.0",
                withLineage,
                "BATCH-NEW02",
                false
        );

        assertThat(outcome.batchResult().batchId()).isEqualTo("BATCH-NEW02");
        assertThat(outcome.batchResult().originalBatchId()).isEqualTo("BATCH-ORIG02");
        assertThat(outcome.batchResult().batchId()).isNotEqualTo(outcome.batchResult().originalBatchId());
    }

    @Test
    void bddCeC05_001_omitsOriginalBatchIdWhenRequestHasNone() {
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(generated("DOC-1"))
                .thenReturn(generated("DOC-2"));

        BatchExecutionService.BatchExecutionOutcome outcome = service.execute(
                template,
                "1.0.0",
                request,
                "BATCH-PLAIN",
                false
        );

        assertThat(outcome.batchResult().originalBatchId()).isNull();
    }

    @Test
    void executeAsyncMode_marksPartialSucceededWhenOneItemFails() {
        when(messageResolver.resolve(anyString())).thenReturn("Generation failed.");
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(generated("DOC-1"))
                .thenThrow(new TemplateValidationException("api.error.rendering.generationFailed"));

        BatchExecutionService.BatchExecutionOutcome outcome = service.execute(
                template,
                "1.0.0",
                request,
                "BATCH-TEST",
                true
        );

        assertThat(outcome.taskStatus()).isEqualTo(TaskStatus.PARTIAL_SUCCEEDED);
        BatchSummaryView summary = outcome.batchResult().summary();
        assertThat(summary.successCount()).isEqualTo(1);
        assertThat(summary.failureCount()).isEqualTo(1);
        verify(idempotencyService).registerDownloadableDocument(any(), anyString(), anyString());
    }

    @Test
    void executeSyncMode_throwsWhenAnyItemFailsWithoutRegisteringDocuments() {
        when(messageResolver.resolve(anyString())).thenReturn("Generation failed.");
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(generated("DOC-1"))
                .thenThrow(new TemplateValidationException("api.error.rendering.generationFailed"));

        assertThatThrownBy(() -> service.execute(template, "1.0.0", request, "BATCH-TEST", false))
                .isInstanceOf(SyncBatchFailureException.class)
                .satisfies(ex -> {
                    SyncBatchFailureException failure = (SyncBatchFailureException) ex;
                    assertThat(failure.batchResult().items()).hasSize(2);
                    assertThat(failure.batchResult().summary().failureCount()).isEqualTo(1);
                });

        verify(idempotencyService, never()).registerDownloadableDocument(any(), anyString(), anyString());
    }

    @Test
    void bddIblA1_007_batchItemVariableValidationFailed_keepsOtherItemSucceeded() {
        when(messageResolver.resolve(VariableValidationException.MESSAGE_KEY))
                .thenReturn("One or more template variables failed validation.");
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(generated("DOC-1"))
                .thenThrow(new VariableValidationException(List.of(
                        new FieldError("customerName", "REQUIRED", "Field is required.")
                )));

        BatchExecutionService.BatchExecutionOutcome outcome = service.execute(
                template,
                "1.0.0",
                request,
                "BATCH-IBL-A1",
                true
        );

        assertThat(outcome.taskStatus()).isEqualTo(TaskStatus.PARTIAL_SUCCEEDED);
        BatchResultItemView ok = outcome.batchResult().items().get(0);
        BatchResultItemView failed = outcome.batchResult().items().get(1);
        assertThat(ok.status()).isEqualTo("SUCCEEDED");
        assertThat(ok.documentId()).isEqualTo("DOC-1");
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.documentId()).isNull();
        assertThat(failed.error().code()).isEqualTo(ApiErrorCodes.VARIABLE_VALIDATION_FAILED);
        assertThat(failed.error().category()).isEqualTo(ApiErrorCategories.VALIDATION);
        assertThat(failed.error().messageKey()).isEqualTo(VariableValidationException.MESSAGE_KEY);
        assertThat(failed.error().fieldErrors()).anySatisfy(error -> {
            assertThat(error.field()).isEqualTo("customerName");
            assertThat(error.reason()).isEqualTo("REQUIRED");
        });
    }

    @Test
    void execute_emitsVariableComputeFailedNotRenderingFailed() {
        when(messageResolver.resolve("api.error.variable.computeFailed"))
                .thenReturn("Variable compute failed.");
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenThrow(new VariableComputeException(
                        "principalCn",
                        "SPELL_AMOUNT(${principal})",
                        "unknown function"
                ));

        BatchExecutionService.BatchExecutionOutcome outcome = service.execute(
                template,
                "1.0.0",
                request,
                "BATCH-COMPUTE",
                true
        );

        assertThat(outcome.taskStatus()).isEqualTo(TaskStatus.FAILED);
        BatchResultItemView failed = outcome.batchResult().items().getFirst();
        ErrorDetail error = failed.error();
        assertThat(error).isNotNull();
        assertThat(error.code()).isEqualTo(ApiErrorCodes.VARIABLE_COMPUTE_FAILED);
        assertThat(error.category()).isEqualTo(ApiErrorCategories.GENERATION);
        assertThat(error.messageKey()).isEqualTo("api.error.variable.computeFailed");
        assertThat(error.retryable()).isFalse();
        assertThat(error.idempotencyConflict())
                .containsEntry("variableKey", "principalCn")
                .containsKey("expressionSummary");
        assertThat(error.code()).isNotEqualTo(ApiErrorCodes.RENDERING_FAILED);
    }

    private DocumentGenerationEngine.GeneratedDocument generated(String documentId) {
        return new DocumentGenerationEngine.GeneratedDocument(
                documentId,
                "generated/" + documentId + "/out.docx",
                new byte[]{1},
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX",
                List.of()
        );
    }
}
