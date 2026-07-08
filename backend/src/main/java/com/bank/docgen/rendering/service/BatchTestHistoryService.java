package com.bank.docgen.rendering.service;

import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.rendering.api.BatchTestRunSummaryView;
import com.bank.docgen.rendering.domain.BatchTestRunStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.service.TemplateService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchTestHistoryService {

    private final TemplateService templateService;
    private final BatchTestRunRepository batchTestRunRepository;
    private final ManagementUserDisplayService managementUserDisplayService;

    public BatchTestHistoryService(
            TemplateService templateService,
            BatchTestRunRepository batchTestRunRepository,
            ManagementUserDisplayService managementUserDisplayService
    ) {
        this.templateService = templateService;
        this.batchTestRunRepository = batchTestRunRepository;
        this.managementUserDisplayService = managementUserDisplayService;
    }

    @Transactional(readOnly = true)
    public List<BatchTestRunSummaryView> listRecentRuns(
            UUID templateId,
            int limit,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        return enrichSummaries(batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId)
                .stream()
                .limit(limit)
                .map(this::toSummaryView)
                .toList());
    }

    private List<BatchTestRunSummaryView> enrichSummaries(List<BatchTestRunSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(BatchTestRunSummaryView::createdBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new BatchTestRunSummaryView(
                        summary.runId(),
                        summary.createdAt(),
                        summary.createdBy(),
                        summary.createdBy() == null ? null : displayNames.get(summary.createdBy()),
                        summary.status(),
                        summary.successCount(),
                        summary.failedCount(),
                        summary.totalCount(),
                        summary.anchorCoveragePct(),
                        summary.variableCoveragePct(),
                        summary.sampleCoveragePct(),
                        summary.gatePassed(),
                        summary.invalidatedAt()
                ))
                .toList();
    }

    private BatchTestRunSummaryView toSummaryView(BatchTestRunEntity run) {
        String status = resolveDisplayStatus(run);
        return new BatchTestRunSummaryView(
                run.getId().toString(),
                run.getCreatedAt(),
                run.getCreatedBy(),
                null,
                status,
                run.getSucceededCount(),
                run.getFailedCount(),
                run.getTotalSamples(),
                run.getAnchorCoveragePct(),
                run.getVariableCoveragePct(),
                run.getSampleCoveragePct(),
                run.getGatePassed(),
                run.getInvalidatedAt()
        );
    }

    private String resolveDisplayStatus(BatchTestRunEntity run) {
        if (run.getInvalidatedAt() != null) {
            return "INVALIDATED";
        }
        if (run.getStatus() == BatchTestRunStatus.RUNNING) {
            return "RUNNING";
        }
        if (run.getStatus() == BatchTestRunStatus.FAILED) {
            return "FAILED";
        }
        return "COMPLETED";
    }
}
