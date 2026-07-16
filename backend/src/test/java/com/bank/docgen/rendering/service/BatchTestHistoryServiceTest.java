package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.rendering.api.BatchTestHistorySampleResultView;
import com.bank.docgen.rendering.api.BatchTestRunSummaryView;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchTestHistoryServiceTest {

    @Mock
    private TemplatePreviewAuthorizationPort previewAuthorizationPort;
    @Mock
    private BatchTestRunRepository batchTestRunRepository;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    private BatchTestHistoryService service;
    private UUID templateId;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new BatchTestHistoryService(
                previewAuthorizationPort,
                batchTestRunRepository,
                managementUserDisplayService,
                new ObjectMapper()
        );
        templateId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000001", "Author", "author@test.com",
                AuthSource.LOCAL, List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"), "route.home", List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        lenient().when(previewAuthorizationPort.requireReadableSnapshot(templateId, session))
                .thenReturn(new RenderableTemplateSnapshot(templateId, UUID.randomUUID(), "RETAIL"));
    }

    @Test
    void listRecentRuns_returnsVisibleRunsInOrder() {
        BatchTestRunEntity run1 = completedRun(templateId, "[]");
        BatchTestRunEntity run2 = completedRun(templateId, "[]");
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(run1, run2));

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).isEqualTo("COMPLETED");
    }

    @Test
    void listRecentRuns_invalidatedRun_showsInvalidatedStatus() {
        BatchTestRunEntity run = completedRun(templateId, "[]");
        run.invalidate();
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(run));

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("INVALIDATED");
        assertThat(result.get(0).invalidatedAt()).isNotNull();
    }

    @Test
    void listRecentRuns_respectsLimit() {
        List<BatchTestRunEntity> runs = List.of(
                completedRun(templateId, "[]"),
                completedRun(templateId, "[]"),
                completedRun(templateId, "[]")
        );
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(runs);

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 2, session);

        assertThat(result).hasSize(2);
    }

    @Test
    void listRecentRuns_emptyList_returnsEmpty() {
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of());

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        assertThat(result).isEmpty();
    }

    @Test
    void listRecentRuns_mapsSampleResultsFromPersistedJson() {
        String sampleJson = """
                [
                  {"dataSetExternalId":"ds-ok","success":true,"docxKey":"k1","pdfKey":"p1"},
                  {"dataSetExternalId":"ds-fail","success":false,"errorDetail":"render failed"}
                ]
                """;
        BatchTestRunEntity run = completedRun(templateId, sampleJson);
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(run));

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        assertThat(result).hasSize(1);
        List<BatchTestHistorySampleResultView> samples = result.get(0).sampleResults();
        assertThat(samples).hasSize(2);
        assertThat(samples.get(0).dataSetExternalId()).isEqualTo("ds-ok");
        assertThat(samples.get(0).success()).isTrue();
        assertThat(samples.get(0).docxKey()).isEqualTo("k1");
        assertThat(samples.get(0).pdfKey()).isEqualTo("p1");
        assertThat(samples.get(0).errorDetail()).isNull();
        assertThat(samples.get(1).dataSetExternalId()).isEqualTo("ds-fail");
        assertThat(samples.get(1).success()).isFalse();
        assertThat(samples.get(1).errorDetail()).isEqualTo("render failed");
    }

    @Test
    void listRecentRuns_legacySyncShapedJson_passesThroughKnownFields() {
        String legacyJson = """
                [
                  {
                    "testDataSetId":"legacy-ds-1",
                    "previewId":"11111111-1111-1111-1111-111111111111",
                    "status":"SUCCEEDED",
                    "warningCount":0,
                    "blockerCount":0
                  }
                ]
                """;
        BatchTestRunEntity run = completedRun(templateId, legacyJson);
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(run));

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        BatchTestHistorySampleResultView sample = result.get(0).sampleResults().get(0);
        assertThat(sample.testDataSetId()).isEqualTo("legacy-ds-1");
        assertThat(sample.previewId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(sample.status()).isEqualTo("SUCCEEDED");
        assertThat(sample.dataSetExternalId()).isNull();
        assertThat(sample.success()).isNull();
    }

    @Test
    void listRecentRuns_nullOrEmptySampleResultsJson_returnsEmptySampleResults() {
        BatchTestRunEntity emptyJson = completedRun(templateId, "[]");
        BatchTestRunEntity running = BatchTestRunEntity.startNew(
                UUID.randomUUID(), templateId, UUID.randomUUID(), "author", 2
        );
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(emptyJson, running));

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        assertThat(result.get(0).sampleResults()).isEmpty();
        assertThat(result.get(1).status()).isEqualTo("RUNNING");
        assertThat(result.get(1).sampleResults()).isEmpty();
    }

    @Test
    void listRecentRuns_malformedSampleResultsJson_keepsSummaryAndReturnsEmptySamples() {
        BatchTestRunEntity run = completedRun(templateId, "{not-valid-json");
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(run));

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("COMPLETED");
        assertThat(result.get(0).successCount()).isEqualTo(3);
        assertThat(result.get(0).sampleResults()).isEmpty();
    }

    @Test
    void listRecentRuns_requiresReadableSnapshot() {
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of());

        service.listRecentRuns(templateId, 5, session);

        verify(previewAuthorizationPort).requireReadableSnapshot(templateId, session);
    }

    @Test
    void listRecentRuns_unreadableTemplate_propagatesAccessDenied() {
        doThrow(new TemplateAccessDeniedException())
                .when(previewAuthorizationPort).requireReadableSnapshot(templateId, session);

        assertThatThrownBy(() -> service.listRecentRuns(templateId, 5, session))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    private BatchTestRunEntity completedRun(UUID templateId, String sampleResultsJson) {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), templateId, UUID.randomUUID(), "author", 3
        );
        run.completeRun(
                3, 0, 0, 0, sampleResultsJson,
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(100),
                true, true
        );
        return run;
    }
}
