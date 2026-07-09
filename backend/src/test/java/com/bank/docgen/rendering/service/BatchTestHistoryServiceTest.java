package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.rendering.api.BatchTestRunSummaryView;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
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
        service = new BatchTestHistoryService(previewAuthorizationPort, batchTestRunRepository, managementUserDisplayService);
        templateId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000001", "Author", "author@test.com",
                AuthSource.LOCAL, List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"), "route.home", List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        when(previewAuthorizationPort.requireReadableSnapshot(templateId, session))
                .thenReturn(new RenderableTemplateSnapshot(templateId, UUID.randomUUID(), "RETAIL"));
    }

    @Test
    void listRecentRuns_returnsVisibleRunsInOrder() {
        BatchTestRunEntity run1 = completedRun(templateId, false);
        BatchTestRunEntity run2 = completedRun(templateId, false);
        when(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(run1, run2));

        List<BatchTestRunSummaryView> result = service.listRecentRuns(templateId, 5, session);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).isEqualTo("COMPLETED");
    }

    @Test
    void listRecentRuns_invalidatedRun_showsInvalidatedStatus() {
        BatchTestRunEntity run = completedRun(templateId, false);
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
                completedRun(templateId, false),
                completedRun(templateId, false),
                completedRun(templateId, false)
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

    private BatchTestRunEntity completedRun(UUID templateId, boolean failed) {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), templateId, UUID.randomUUID(), "author", 3
        );
        run.completeRun(
                3, 0, 0, 0, "[]",
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(100),
                true, true
        );
        return run;
    }
}
