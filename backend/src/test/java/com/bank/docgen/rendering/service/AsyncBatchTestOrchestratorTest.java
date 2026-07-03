package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.rendering.api.AsyncBatchStartResponse;
import com.bank.docgen.rendering.api.PreviewComparisonView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.domain.BatchTestRunStatus;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageDimensionView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.service.CoverageComputationService;
import com.bank.docgen.template.service.CoverageThresholdResolver;
import com.bank.docgen.template.service.TemplateCurrentVersionResolver;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.atLeast;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsyncBatchTestOrchestratorTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private PreviewGenerationService previewGenerationService;
    @Mock
    private BatchTestRunRepository batchTestRunRepository;
    @Mock
    private TestDataSetRepository testDataSetRepository;
    @Mock
    private CoverageComputationService coverageComputationService;
    @Mock
    private CoverageThresholdResolver coverageThresholdResolver;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;

    private SseEmitterRegistry batchSseRegistry;
    private AsyncBatchTestOrchestrator orchestrator;
    private UUID templateId;
    private UUID versionId;
    private ManagementSessionClaims session;
    private final Executor syncExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        batchSseRegistry = new SseEmitterRegistry();
        orchestrator = new AsyncBatchTestOrchestrator(
                templateService, groupAccessService, previewGenerationService,
                batchTestRunRepository, testDataSetRepository,
                coverageComputationService, coverageThresholdResolver,
                templateCurrentVersionResolver, batchSseRegistry,
                new ObjectMapper(), syncExecutor
        );
        templateId = UUID.randomUUID();
        versionId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000001", "Author", "author@test.com",
                AuthSource.LOCAL, List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"), "route.home", List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        when(templateService.requireReadableTemplate(templateId, session))
                .thenReturn(new TemplateEntity(templateId, "TPL-1", "RETAIL", "Demo", null, UUID.randomUUID(), "author"));
        when(groupAccessService.canAuthorTemplates(session)).thenReturn(true);
        when(templateCurrentVersionResolver.requireInFlightDevVersion(templateId))
                .thenReturn(new TemplateVersionEntity(versionId, templateId, "author"));
        when(batchTestRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(batchTestRunRepository.findById(any())).thenReturn(Optional.empty());
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(any()))
                .thenReturn(List.of());
    }

    @Test
    void startBatchRun_returnsRunIdAndStreamUrl() {
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId))
                .thenReturn(List.of());
        when(coverageComputationService.compute(any(), any())).thenReturn(emptyCoverage());

        AsyncBatchStartResponse response = orchestrator.startBatchRun(templateId, session, "http://localhost");

        assertThat(response.runId()).isNotBlank();
        assertThat(response.streamUrl()).contains("/progress-stream");
    }

    @Test
    void startBatchRun_withDataSets_processesAllSamples() {
        TestDataSetEntity ds1 = mockDataSet("TDS-001");
        TestDataSetEntity ds2 = mockDataSet("TDS-002");
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId))
                .thenReturn(List.of(ds1, ds2));
        stubSuccessfulGeneration("TDS-001");
        stubSuccessfulGeneration("TDS-002");
        when(coverageComputationService.compute(any(), any())).thenReturn(emptyCoverage());

        orchestrator.startBatchRun(templateId, session, "http://localhost");

        verify(previewGenerationService).runTestGenerateForBatch(eq(templateId), eq("TDS-001"), any(), eq(session));
        verify(previewGenerationService).runTestGenerateForBatch(eq(templateId), eq("TDS-002"), any(), eq(session));
    }

    @Test
    void startBatchRun_savesRunEntity() {
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId))
                .thenReturn(List.of());
        when(coverageComputationService.compute(any(), any())).thenReturn(emptyCoverage());

        orchestrator.startBatchRun(templateId, session, "http://localhost");

        // save is called at least twice: once for RUNNING state (initial), once for COMPLETED state (after execution)
        ArgumentCaptor<BatchTestRunEntity> captor = ArgumentCaptor.forClass(BatchTestRunEntity.class);
        verify(batchTestRunRepository, atLeast(1)).save(captor.capture());
        // The first saved entity has versionId set; the entity is mutated in-place to COMPLETED after async execution
        BatchTestRunEntity firstSaved = captor.getAllValues().get(0);
        assertThat(firstSaved.getTemplateVersionId()).isEqualTo(versionId);
        // After sync executor completes, entity ends in COMPLETED state
        assertThat(captor.getValue().getStatus()).isEqualTo(BatchTestRunStatus.COMPLETED);
    }

    @Test
    void startBatchRun_prunesRunsExceedingFive() {
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId))
                .thenReturn(List.of());
        when(coverageComputationService.compute(any(), any())).thenReturn(emptyCoverage());

        BatchTestRunEntity old1 = runEntity();
        BatchTestRunEntity old2 = runEntity();
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(runEntity(), runEntity(), runEntity(), runEntity(), runEntity(), old1, old2));

        orchestrator.startBatchRun(templateId, session, "http://localhost");

        // Both old1 and old2 should be hidden
        assertThat(old1.isHidden()).isTrue();
        assertThat(old2.isHidden()).isTrue();
    }

    private TestDataSetEntity mockDataSet(String externalId) {
        TestDataSetEntity ds = org.mockito.Mockito.mock(TestDataSetEntity.class);
        when(ds.getExternalId()).thenReturn(externalId);
        return ds;
    }

    private void stubSuccessfulGeneration(String dataSetId) {
        when(previewGenerationService.runTestGenerateForBatch(eq(templateId), eq(dataSetId), any(), eq(session)))
                .thenReturn(new PreviewRecordView(
                        UUID.randomUUID().toString(), templateId.toString(), versionId.toString(),
                        PreviewStatus.SUCCEEDED, "DOCX", "rp-v1",
                        "batch-test/run/ds.docx", "batch-test/run/ds.pdf",
                        List.of(), new PreviewComparisonView(0, 0, 0, List.of()),
                        dataSetId, Instant.now()
                ));
    }

    private CoverageSummaryView emptyCoverage() {
        List<CoverageDimensionView> dims = List.of(
                new CoverageDimensionView(CoverageComputationService.DIMENSION_REQUIRED_VARIABLES, 0, 0, 100, 80, false),
                new CoverageDimensionView(CoverageComputationService.DIMENSION_REQUIRED_SAMPLES, 0, 0, 100, 100, false),
                new CoverageDimensionView(CoverageComputationService.DIMENSION_ANCHOR_BINDINGS, 0, 0, 100, 80, false)
        );
        return new CoverageSummaryView(templateId.toString(), 100, false, List.of(), dims,
                new CoverageThresholdView("GLOBAL", null, 80, 100, 80));
    }

    private BatchTestRunEntity runEntity() {
        return BatchTestRunEntity.startNew(UUID.randomUUID(), templateId, versionId, "author", 0);
    }
}
