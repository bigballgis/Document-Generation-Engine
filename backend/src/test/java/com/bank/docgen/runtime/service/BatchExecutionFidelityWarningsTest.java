package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.FidelityWarning;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-C03-001/003: batch execution and task-queryable batch results expose full
 * FidelityWarning objects on succeeded items.
 */
@ExtendWith(MockitoExtension.class)
class BatchExecutionFidelityWarningsTest {

    @Mock
    private DocumentGenerationEngine documentGenerationEngine;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private MessageResolver messageResolver;

    private BatchExecutionService service;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        RuntimeFidelityWarningMapper mapper = new RuntimeFidelityWarningMapper(messageResolver);
        service = new BatchExecutionService(
                documentGenerationEngine,
                idempotencyService,
                messageResolver,
                mapper
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
        when(messageResolver.resolveOrDefault(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        lenient().when(messageResolver.resolveOrDefault(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void bddCeC03_001_succeededItemsCarryFullFidelityWarningObjects() {
        when(documentGenerationEngine.generate(
                any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(generated(
                        "DOC-1",
                        List.of(FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name())
                ));

        BatchGenerateRequestBody request = singleItemRequest();
        BatchExecutionService.BatchExecutionOutcome outcome = service.execute(
                template,
                "1.0.0",
                request,
                "BATCH-FW",
                true
        );

        BatchResultItemView item = outcome.batchResult().items().getFirst();
        assertThat(item.status()).isEqualTo("SUCCEEDED");
        assertThat(item.fidelityWarnings()).hasSize(1);
        FidelityWarning warning = item.fidelityWarnings().getFirst();
        assertThat(warning).isInstanceOf(FidelityWarning.class);
        assertThat(warning.warningCode()).isEqualTo("CONTROLLED_STYLE_FALLBACK");
        assertThat(warning.messageKey()).isEqualTo("generation.warning.fidelity.controlledStyleFallback");
        assertThat(warning.sensitiveDataExcluded()).isTrue();
        assertThat(warning.message()).isNotBlank();
        assertThat(warning.locationSummary()).isNotBlank();
        assertThat(warning.detectedSummary()).isNotBlank();
        assertThat(warning.recommendation()).isNotBlank();
    }

    @Test
    void bddCeC03_002_noWarningsYieldEmptyArray() {
        when(documentGenerationEngine.generate(
                any(), anyString(), any(), anyString(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(generated("DOC-2", List.of()));

        BatchResultItemView item = service.execute(
                template,
                "1.0.0",
                singleItemRequest(),
                "BATCH-EMPTY",
                true
        ).batchResult().items().getFirst();

        assertThat(item.fidelityWarnings()).isEmpty();
    }

    private BatchGenerateRequestBody singleItemRequest() {
        return new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody(
                        "item-1",
                        Map.of("customerName", "Alice"),
                        null,
                        null
                )),
                new EncryptionOptionsView(false, null, null, List.of()),
                "req-1",
                "idem-1",
                null,
                null
        );
    }

    private DocumentGenerationEngine.GeneratedDocument generated(String documentId, List<String> codes) {
        return new DocumentGenerationEngine.GeneratedDocument(
                documentId,
                "generated/" + documentId + "/out.docx",
                new byte[]{1},
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX",
                codes
        );
    }
}
