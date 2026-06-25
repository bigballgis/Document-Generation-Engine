package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.BatchTestGenerateRequest;
import com.bank.docgen.rendering.api.BatchTestSampleResultView;
import com.bank.docgen.rendering.api.BatchTestSummaryView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchTestGenerationService {

    private final TemplateService templateService;
    private final PreviewGenerationService previewGenerationService;
    private final BatchTestRunRepository batchTestRunRepository;
    private final GroupAccessService groupAccessService;
    private final ObjectMapper objectMapper;

    public BatchTestGenerationService(
            TemplateService templateService,
            PreviewGenerationService previewGenerationService,
            BatchTestRunRepository batchTestRunRepository,
            GroupAccessService groupAccessService,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.previewGenerationService = previewGenerationService;
        this.batchTestRunRepository = batchTestRunRepository;
        this.groupAccessService = groupAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BatchTestSummaryView runBatch(
            UUID templateId,
            BatchTestGenerateRequest request,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }

        UUID batchRunId = UUID.randomUUID();
        List<BatchTestSampleResultView> samples = new ArrayList<>();
        int succeededCount = 0;
        int failedCount = 0;
        int warningCount = 0;
        int blockerCount = 0;

        for (String testDataSetId : request.testDataSetIds()) {
            PreviewRecordView preview = previewGenerationService.runTestGenerateForBatch(
                    templateId,
                    testDataSetId,
                    batchRunId,
                    session
            );
            int sampleWarnings = preview.fidelityWarnings() == null ? 0 : preview.fidelityWarnings().size();
            int sampleBlockers = preview.status() == PreviewStatus.FAILED ? 1 : 0;
            if (preview.status() == PreviewStatus.SUCCEEDED) {
                succeededCount++;
            } else {
                failedCount++;
            }
            warningCount += sampleWarnings;
            blockerCount += sampleBlockers;
            samples.add(new BatchTestSampleResultView(
                    testDataSetId,
                    preview.previewId(),
                    preview.status(),
                    sampleWarnings,
                    sampleBlockers
            ));
        }

        BatchTestRunEntity run = new BatchTestRunEntity(
                batchRunId,
                templateId,
                session.username(),
                samples.size(),
                succeededCount,
                failedCount,
                warningCount,
                blockerCount,
                writeSummaryJson(samples)
        );
        batchTestRunRepository.save(run);

        return new BatchTestSummaryView(
                run.getId().toString(),
                templateId.toString(),
                run.getTotalSamples(),
                run.getSucceededCount(),
                run.getFailedCount(),
                run.getWarningCount(),
                run.getBlockerCount(),
                samples,
                run.getCreatedAt()
        );
    }

    private String writeSummaryJson(List<BatchTestSampleResultView> samples) {
        try {
            return objectMapper.writeValueAsString(samples);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
