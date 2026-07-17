package com.bank.docgen.rendering.service;

import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.rendering.api.BatchTestHistorySampleResultView;
import com.bank.docgen.rendering.api.BatchTestRunSummaryView;
import com.bank.docgen.rendering.domain.BatchTestRunStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchTestHistoryService {

    private static final Logger LOG = LoggerFactory.getLogger(BatchTestHistoryService.class);

    private final TemplatePreviewAuthorizationPort previewAuthorizationPort;
    private final BatchTestRunRepository batchTestRunRepository;
    private final ManagementUserDisplayService managementUserDisplayService;
    private final ObjectMapper objectMapper;

    public BatchTestHistoryService(
            TemplatePreviewAuthorizationPort previewAuthorizationPort,
            BatchTestRunRepository batchTestRunRepository,
            ManagementUserDisplayService managementUserDisplayService,
            ObjectMapper objectMapper
    ) {
        this.previewAuthorizationPort = previewAuthorizationPort;
        this.batchTestRunRepository = batchTestRunRepository;
        this.managementUserDisplayService = managementUserDisplayService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BatchTestRunSummaryView> listRecentRuns(
            UUID templateId,
            int limit,
            ManagementSessionClaims session
    ) {
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        if (limit <= 0) {
            return List.of();
        }
        // PRR-A02: TopN at DB — do not load-all then stream().limit.
        return enrichSummaries(batchTestRunRepository
                .findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId, PageRequest.of(0, limit))
                .stream()
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
                        summary.invalidatedAt(),
                        summary.sampleResults()
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
                run.getInvalidatedAt(),
                parseSampleResults(run.getSampleResultsJson())
        );
    }

    private List<BatchTestHistorySampleResultView> parseSampleResults(String sampleResultsJson) {
        if (sampleResultsJson == null || sampleResultsJson.isBlank()) {
            return List.of();
        }
        try {
            List<BatchTestHistorySampleResultView> parsed = objectMapper.readValue(
                    sampleResultsJson,
                    new TypeReference<>() {}
            );
            return parsed == null ? List.of() : List.copyOf(parsed);
        } catch (JsonProcessingException | RuntimeException ex) {
            LOG.debug("Failed to parse batch-test sampleResultsJson: {}", ex.getMessage());
            return List.of();
        }
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
