package com.bank.docgen.runtime.service;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncBatchTaskStaleReclaimService {

    public static final String STALE_TASK_RECLAIMED = "STALE_TASK_RECLAIMED";
    public static final String STALE_TASK_RECLAIM_EXHAUSTED = "STALE_TASK_RECLAIM_EXHAUSTED";
    public static final int MAX_RECLAIM_ATTEMPTS = 3;

    private static final Logger LOG = LoggerFactory.getLogger(AsyncBatchTaskStaleReclaimService.class);

    private final GenerationAsyncTaskRepository asyncTaskRepository;
    private final AsyncBatchTaskDispatcher asyncBatchTaskDispatcher;
    private final DocgenAsyncProperties asyncProperties;
    private final AsyncBatchPayloadScrubber payloadScrubber;
    private final ObjectMapper objectMapper;

    public AsyncBatchTaskStaleReclaimService(
            GenerationAsyncTaskRepository asyncTaskRepository,
            @Lazy AsyncBatchTaskDispatcher asyncBatchTaskDispatcher,
            DocgenAsyncProperties asyncProperties,
            AsyncBatchPayloadScrubber payloadScrubber,
            ObjectMapper objectMapper
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.asyncBatchTaskDispatcher = asyncBatchTaskDispatcher;
        this.asyncProperties = asyncProperties;
        this.payloadScrubber = payloadScrubber;
        this.objectMapper = objectMapper;
    }

    public boolean isStale(GenerationAsyncTaskEntity task) {
        return task.getStatus() == TaskStatus.PROCESSING
                && task.getUpdatedAt().isBefore(staleCutoff());
    }

    public Instant staleCutoff() {
        return Instant.now().minusSeconds(asyncProperties.getStaleProcessingThresholdSeconds());
    }

    @Transactional
    public int reclaimStaleTasks() {
        List<GenerationAsyncTaskEntity> staleTasks = asyncTaskRepository.findByStatusAndUpdatedAtBefore(
                TaskStatus.PROCESSING,
                staleCutoff()
        );
        int reclaimed = 0;
        for (GenerationAsyncTaskEntity task : staleTasks) {
            if (reclaimTask(task.getId())) {
                reclaimed++;
            }
        }
        return reclaimed;
    }

    @Transactional
    public boolean reclaimTask(UUID taskId) {
        GenerationAsyncTaskEntity task = asyncTaskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != TaskStatus.PROCESSING || !isStale(task)) {
            return false;
        }
        Instant expectedUpdatedAt = task.getUpdatedAt();
        if (task.getProcessingAttemptCount() >= MAX_RECLAIM_ATTEMPTS) {
            return failExhausted(task, expectedUpdatedAt);
        }
        return resetAndRedispatch(task, expectedUpdatedAt);
    }

    private boolean failExhausted(GenerationAsyncTaskEntity task, Instant expectedUpdatedAt) {
        int updated = asyncTaskRepository.compareAndSetStatus(
                task.getId(),
                TaskStatus.FAILED,
                Instant.now(),
                TaskStatus.PROCESSING,
                expectedUpdatedAt
        );
        if (updated == 0) {
            return false;
        }
        GenerationAsyncTaskEntity refreshed = asyncTaskRepository.findById(task.getId()).orElseThrow();
        scrubTerminalPayload(refreshed);
        asyncTaskRepository.save(refreshed);
        LOG.warn(
                "[AsyncBatchStaleReclaim] Task {} marked FAILED after {} processing attempts ({})",
                task.getId(),
                refreshed.getProcessingAttemptCount(),
                STALE_TASK_RECLAIM_EXHAUSTED
        );
        return true;
    }

    private boolean resetAndRedispatch(GenerationAsyncTaskEntity task, Instant expectedUpdatedAt) {
        int updated = asyncTaskRepository.compareAndSetStatus(
                task.getId(),
                TaskStatus.ACCEPTED,
                Instant.now(),
                TaskStatus.PROCESSING,
                expectedUpdatedAt
        );
        if (updated == 0) {
            return false;
        }
        LOG.info(
                "[AsyncBatchStaleReclaim] Task {} reset to ACCEPTED and re-dispatched ({})",
                task.getId(),
                STALE_TASK_RECLAIMED
        );
        asyncBatchTaskDispatcher.dispatch(task.getId());
        return true;
    }

    private void scrubTerminalPayload(GenerationAsyncTaskEntity task) {
        try {
            BatchGenerateRequestBody request = objectMapper.readValue(
                    task.getRequestPayloadJson(),
                    BatchGenerateRequestBody.class
            );
            task.scrubRequestPayload(payloadScrubber.scrub(request));
        } catch (Exception ex) {
            LOG.warn("[AsyncBatchStaleReclaim] Failed to scrub payload for task {}: {}", task.getId(), ex.getMessage());
        }
    }
}
