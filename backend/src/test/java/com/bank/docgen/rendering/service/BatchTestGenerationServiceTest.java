package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.rendering.api.BatchTestGenerateRequest;
import com.bank.docgen.rendering.api.BatchTestSummaryView;
import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchTestGenerationServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private PreviewGenerationService previewGenerationService;
    @Mock
    private BatchTestRunRepository batchTestRunRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private BatchTestGenerationService service;
    private UUID templateId;
    private ManagementSessionClaims author;

    @BeforeEach
    void setUp() {
        service = new BatchTestGenerationService(
                templateService,
                previewGenerationService,
                batchTestRunRepository,
                groupAccessService,
                new ObjectMapper()
        );
        templateId = UUID.randomUUID();
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
        when(templateService.requireReadableTemplate(templateId, author))
                .thenReturn(new TemplateEntity(
                        templateId,
                        "TPL-1",
                        "RETAIL",
                        "Demo",
                        null,
                        UUID.randomUUID(),
                        "10000003"
                ));
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(batchTestRunRepository.save(any(BatchTestRunEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void batchTest_overThreeSamples_createsThreePreviewRecords() {
        stubPreview("TDS-001", PreviewStatus.SUCCEEDED, 1);
        stubPreview("TDS-002", PreviewStatus.SUCCEEDED, 1);
        stubPreview("TDS-003", PreviewStatus.SUCCEEDED, 1);

        BatchTestSummaryView summary = service.runBatch(
                templateId,
                new BatchTestGenerateRequest(List.of("TDS-001", "TDS-002", "TDS-003")),
                author
        );

        verify(previewGenerationService).runTestGenerateForBatch(
                eq(templateId), eq("TDS-001"), any(UUID.class), eq(author));
        verify(previewGenerationService).runTestGenerateForBatch(
                eq(templateId), eq("TDS-002"), any(UUID.class), eq(author));
        verify(previewGenerationService).runTestGenerateForBatch(
                eq(templateId), eq("TDS-003"), any(UUID.class), eq(author));
        assertThat(summary.totalSamples()).isEqualTo(3);
        assertThat(summary.samples()).hasSize(3);
        assertThat(summary.succeededCount()).isEqualTo(3);
    }

    @Test
    void batchTest_summary_aggregatesWarningsAndBlockers() {
        when(previewGenerationService.runTestGenerateForBatch(eq(templateId), eq("TDS-001"), any(UUID.class), eq(author)))
                .thenReturn(previewView("preview-1", "TDS-001", PreviewStatus.SUCCEEDED, 2));
        when(previewGenerationService.runTestGenerateForBatch(eq(templateId), eq("TDS-002"), any(UUID.class), eq(author)))
                .thenReturn(previewView("preview-2", "TDS-002", PreviewStatus.SUCCEEDED, 1));

        BatchTestSummaryView summary = service.runBatch(
                templateId,
                new BatchTestGenerateRequest(List.of("TDS-001", "TDS-002")),
                author
        );

        assertThat(summary.warningCount()).isEqualTo(3);
        assertThat(summary.blockerCount()).isZero();
        assertThat(summary.succeededCount()).isEqualTo(2);

        ArgumentCaptor<BatchTestRunEntity> captor = ArgumentCaptor.forClass(BatchTestRunEntity.class);
        verify(batchTestRunRepository).save(captor.capture());
        assertThat(captor.getValue().getWarningCount()).isEqualTo(3);
    }

    @Test
    void batchTest_oneSampleFails_summaryReflectsFailure() {
        when(previewGenerationService.runTestGenerateForBatch(eq(templateId), eq("TDS-OK"), any(UUID.class), eq(author)))
                .thenReturn(previewView("preview-ok", "TDS-OK", PreviewStatus.SUCCEEDED, 1));
        when(previewGenerationService.runTestGenerateForBatch(eq(templateId), eq("TDS-FAIL"), any(UUID.class), eq(author)))
                .thenReturn(previewView("preview-fail", "TDS-FAIL", PreviewStatus.FAILED, 0));

        BatchTestSummaryView summary = service.runBatch(
                templateId,
                new BatchTestGenerateRequest(List.of("TDS-OK", "TDS-FAIL")),
                author
        );

        assertThat(summary.succeededCount()).isEqualTo(1);
        assertThat(summary.failedCount()).isEqualTo(1);
        assertThat(summary.blockerCount()).isEqualTo(1);
        assertThat(summary.samples().get(1).status()).isEqualTo(PreviewStatus.FAILED);
    }

    private void stubPreview(String testDataSetId, PreviewStatus status, int warningCount) {
        when(previewGenerationService.runTestGenerateForBatch(
                eq(templateId), eq(testDataSetId), any(UUID.class), eq(author)))
                .thenReturn(previewView("preview-" + testDataSetId, testDataSetId, status, warningCount));
    }

    private PreviewRecordView previewView(
            String previewId,
            String testDataSetId,
            PreviewStatus status,
            int warningCount
    ) {
        List<FidelityWarningView> warnings = java.util.stream.IntStream.range(0, warningCount)
                .mapToObj(i -> new FidelityWarningView("WARN_" + i, "generation.warning.fidelity.controlledStyleFallback"))
                .toList();
        return new PreviewRecordView(
                previewId,
                templateId.toString(),
                UUID.randomUUID().toString(),
                status,
                "DOCX",
                status == PreviewStatus.SUCCEEDED ? "previews/" + previewId + "/output.docx" : null,
                warnings,
                "anchorsConfigured=1;warnings=" + warningCount,
                testDataSetId,
                Instant.now()
        );
    }
}
