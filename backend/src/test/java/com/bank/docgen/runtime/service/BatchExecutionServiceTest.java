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
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
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
        service = new BatchExecutionService(documentGenerationEngine, idempotencyService, messageResolver);
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
                "idem-1"
        );
    }

    @Test
    void executeAsyncMode_marksPartialSucceededWhenOneItemFails() {
        when(messageResolver.resolve(anyString())).thenReturn("Generation failed.");
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any()))
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
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any()))
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
