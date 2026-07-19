package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.FidelityWarning;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-C06: DOCX permissions are PDF-only at apply time; DOCX + non-empty permissions
 * succeeds with fidelity warning DOCX_PERMISSIONS_NOT_APPLIED.
 */
@ExtendWith(MockitoExtension.class)
class CeC06DocxPermissionsBoundaryTest {

    private static final String OPEN = "OpenPassword12";
    private static final String OWNER = "OwnerPassword12";

    @Mock
    private DocumentGenerationEngine documentGenerationEngine;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private MessageResolver messageResolver;

    private RuntimeFidelityWarningMapper mapper;
    private BatchExecutionService batchService;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        lenient().when(messageResolver.resolveOrDefault(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        mapper = new RuntimeFidelityWarningMapper(messageResolver);
        batchService = new BatchExecutionService(
                documentGenerationEngine,
                idempotencyService,
                messageResolver,
                mapper
        );
        template = new TemplateEntity(
                UUID.randomUUID(),
                "TPL-C06",
                "RETAIL",
                "CE-C06",
                null,
                UUID.randomUUID(),
                "10000001"
        );
    }

    @Test
    void bddCeC06_002_mapperExposesDocxPermissionsNotAppliedWarning() {
        List<FidelityWarning> warnings = mapper.toWarnings(List.of(
                FidelityWarningCode.DOCX_PERMISSIONS_NOT_APPLIED.name()
        ));

        assertThat(warnings).hasSize(1);
        FidelityWarning warning = warnings.getFirst();
        assertThat(warning.warningCode()).isEqualTo("DOCX_PERMISSIONS_NOT_APPLIED");
        assertThat(warning.messageKey())
                .isEqualTo("generation.warning.fidelity.docxPermissionsNotApplied");
        assertThat(warning.sensitiveDataExcluded()).isTrue();
        assertThat(warning.message()).doesNotContainIgnoringCase("password");
        assertThat(warning.detectedSummary()).doesNotContainIgnoringCase("OpenPassword");
        assertThat(warning.detectedSummary()).doesNotContain(OWNER);
    }

    @Test
    void bddCeC06_010_encryptionSummaryEchoesRequestedPermissionsWithWarning() {
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                OPEN,
                OWNER,
                List.of("ALLOW_PRINT", "ALLOW_COPY")
        );
        EncryptionSummaryView summary = EncryptionSummaryView.fromRequest("DOCX", encryption);
        List<FidelityWarning> warnings = mapper.toWarnings(List.of(
                FidelityWarningCode.DOCX_PERMISSIONS_NOT_APPLIED.name()
        ));

        assertThat(summary.enabled()).isTrue();
        assertThat(summary.outputFormat()).isEqualTo("DOCX");
        assertThat(summary.openPasswordProvided()).isTrue();
        assertThat(summary.ownerPasswordProvided()).isTrue();
        assertThat(summary.permissions()).containsExactly("ALLOW_PRINT", "ALLOW_COPY");
        assertThat(warnings.getFirst().warningCode()).isEqualTo("DOCX_PERMISSIONS_NOT_APPLIED");
    }

    @Test
    void bddCeC06_009_batchMixedFormatsWarnOnlyDocxItem() {
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                OPEN,
                OWNER,
                List.of("ALLOW_PRINT")
        );
        when(documentGenerationEngine.generate(
                any(), anyString(), any(), eq("DOCX"), any(), any(), anyString(), any(), any()))
                .thenReturn(generated(
                        "DOC-DOCX",
                        "DOCX",
                        List.of(FidelityWarningCode.DOCX_PERMISSIONS_NOT_APPLIED.name())
                ));
        when(documentGenerationEngine.generate(
                any(), anyString(), any(), eq("PDF"), any(), any(), anyString(), any(), any()))
                .thenReturn(generated("DOC-PDF", "PDF", List.of()));

        BatchGenerateRequestBody request = new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                List.of(
                        new BatchGenerateRequestBody.BatchGenerateItemBody(
                                "item-docx",
                                Map.of("n", "1"),
                                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                                encryption
                        ),
                        new BatchGenerateRequestBody.BatchGenerateItemBody(
                                "item-pdf",
                                Map.of("n", "2"),
                                new OutputOptionsView("PDF", "ASYNC_TASK"),
                                encryption
                        )
                ),
                null,
                "req-c06",
                "idem-c06",
                null,
                null
        );

        List<BatchResultItemView> items = batchService.execute(
                template,
                "1.0.0",
                request,
                "BATCH-C06",
                true
        ).batchResult().items();

        BatchResultItemView docxItem = items.stream()
                .filter(i -> "item-docx".equals(i.itemId()))
                .findFirst()
                .orElseThrow();
        BatchResultItemView pdfItem = items.stream()
                .filter(i -> "item-pdf".equals(i.itemId()))
                .findFirst()
                .orElseThrow();

        assertThat(docxItem.fidelityWarnings())
                .extracting(FidelityWarning::warningCode)
                .containsExactly("DOCX_PERMISSIONS_NOT_APPLIED");
        assertThat(docxItem.encryptionSummary().permissions()).containsExactly("ALLOW_PRINT");
        assertThat(pdfItem.fidelityWarnings())
                .extracting(FidelityWarning::warningCode)
                .doesNotContain("DOCX_PERMISSIONS_NOT_APPLIED");
        assertThat(pdfItem.encryptionSummary().permissions()).containsExactly("ALLOW_PRINT");
    }

    @Test
    void bddCeC06_011_openApiDeclaresDocxPermissionsNotAppliedAndPdfOnlyPermissions() throws Exception {
        Path openApi = Path.of("docs/api/openapi-v1.yaml");
        if (!Files.isRegularFile(openApi)) {
            openApi = Path.of("../docs/api/openapi-v1.yaml");
        }
        assertThat(openApi).exists();
        String yaml = Files.readString(openApi);

        assertThat(yaml).contains("DOCX_PERMISSIONS_NOT_APPLIED");
        assertThat(FidelityWarningCode.DOCX_PERMISSIONS_NOT_APPLIED.name())
                .isEqualTo("DOCX_PERMISSIONS_NOT_APPLIED");
        assertThat(yaml).containsIgnoringCase("permissions apply only when output.format is PDF");
        assertThat(yaml).contains("Apache POI DOCX");
        assertThat(yaml).containsIgnoringCase("write-protect is out of scope");
    }

    private DocumentGenerationEngine.GeneratedDocument generated(
            String documentId,
            String format,
            List<String> codes
    ) {
        String contentType = "PDF".equals(format)
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return new DocumentGenerationEngine.GeneratedDocument(
                documentId,
                "generated/" + documentId + "/out",
                new byte[]{1},
                contentType,
                format,
                codes
        );
    }
}
