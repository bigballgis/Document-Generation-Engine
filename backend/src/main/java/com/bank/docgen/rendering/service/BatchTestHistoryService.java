package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.BatchTestRunSummaryView;
import com.bank.docgen.rendering.domain.BatchTestRunStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.service.TemplateService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchTestHistoryService {

    private final TemplateService templateService;
    private final BatchTestRunRepository batchTestRunRepository;

    public BatchTestHistoryService(
            TemplateService templateService,
            BatchTestRunRepository batchTestRunRepository
    ) {
        this.templateService = templateService;
        this.batchTestRunRepository = batchTestRunRepository;
    }

    @Transactional(readOnly = true)
    public List<BatchTestRunSummaryView> listRecentRuns(
            UUID templateId,
            int limit,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        return batchTestRunRepository.findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId)
                .stream()
                .limit(limit)
                .map(this::toSummaryView)
                .toList();
    }

    private BatchTestRunSummaryView toSummaryView(BatchTestRunEntity run) {
        String status = resolveDisplayStatus(run);
        return new BatchTestRunSummaryView(
                run.getId().toString(),
                run.getCreatedAt(),
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
