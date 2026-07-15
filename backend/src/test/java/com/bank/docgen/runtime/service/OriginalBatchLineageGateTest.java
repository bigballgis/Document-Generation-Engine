package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
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
 * BDD-CE-C05-008 / 009 — lineage gate order and original immutability (read-only lookup).
 */
@ExtendWith(MockitoExtension.class)
class OriginalBatchLineageGateTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CREDENTIAL_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private OriginalBatchLineageValidator lineageValidator;

    @Mock
    private BatchExecutionService batchExecutionService;

    private TemplateEntity template;
    private RuntimeSessionClaims session;

    @BeforeEach
    void setUp() {
        template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-1",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        session = new RuntimeSessionClaims(
                CREDENTIAL_ID,
                "CRED-1",
                TEMPLATE_ID,
                "TPL-1",
                "10000001",
                List.of("AD-GROUP-A")
        );
    }

    @Test
    void bddCeC05_009_validationFailureDoesNotInvokeBatchExecution() {
        BatchGenerateRequestBody request = new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody(
                        "item-1", Map.of("a", 1), null, null)),
                new EncryptionOptionsView(false, null, null, List.of()),
                "req-1",
                "idem-1",
                "BATCH-MISSING1",
                null
        );

        doThrow(new OriginalBatchNotFoundException())
                .when(lineageValidator)
                .requireValidOriginalBatchIfPresent(eq("BATCH-MISSING1"), eq(CREDENTIAL_ID));

        assertThatThrownBy(() -> {
            lineageValidator.requireValidOriginalBatchIfPresent(request.originalBatchId(), session.credentialId());
            batchExecutionService.execute(template, "1.0.0", request, "BATCH-NEW", false);
        }).isInstanceOf(OriginalBatchNotFoundException.class);

        verify(batchExecutionService, never()).execute(any(), anyString(), any(), anyString(), any(Boolean.class));
    }

    @Test
    void bddCeC05_008_lineageValidationIsReadOnlyLookup() {
        lineageValidator.requireValidOriginalBatchIfPresent("BATCH-ORIG03", CREDENTIAL_ID);

        verify(lineageValidator).requireValidOriginalBatchIfPresent("BATCH-ORIG03", CREDENTIAL_ID);
        assertThat(session.credentialId()).isEqualTo(CREDENTIAL_ID);
    }
}
