package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchGenerateResultView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.TaskSummaryView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private async-task persistence, replay lookup, and task summary mapping.
 */
final class BatchAsyncTaskPersistenceSupport {

    private final GenerationAsyncTaskRepository asyncTaskRepository;
    private final BatchGenerationJsonSupport json;

    BatchAsyncTaskPersistenceSupport(
            GenerationAsyncTaskRepository asyncTaskRepository,
            BatchGenerationJsonSupport json
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.json = json;
    }

    void ensureTaskQueryable(GenerationAsyncTaskEntity task) {
        if (task.getStatus() == TaskStatus.EXPIRED) {
            throw new AsyncTaskExpiredException();
        }
        if (task.getExpiresAt().isAfter(Instant.now())) {
            return;
        }
        if (task.getStatus() == TaskStatus.ACCEPTED || task.getStatus() == TaskStatus.PROCESSING) {
            task.markExpired();
            asyncTaskRepository.save(task);
            throw new AsyncTaskExpiredException();
        }
    }

    void persistBatchTask(
            UUID templateId,
            String taskExternalId,
            String batchExternalId,
            TaskStatus status,
            String routeType,
            String releaseVersion,
            BatchGenerateRequestBody request,
            String requestHash,
            BatchResultView batchResult
    ) {
        GenerationAsyncTaskEntity entity = new GenerationAsyncTaskEntity(
                UUID.randomUUID(),
                taskExternalId,
                batchExternalId,
                templateId,
                status,
                routeType,
                releaseVersion,
                request.requestId(),
                request.idempotencyKey(),
                requestHash,
                json.writeRequestPayload(request),
                Instant.now().plusSeconds(IdempotencyConstants.RETENTION_SECONDS)
        );
        String batchResultJson = json.writeBatchResult(batchResult);
        if (status == TaskStatus.FAILED) {
            entity.markFailed(batchResultJson);
        } else {
            entity.markSucceeded(batchResultJson);
        }
        asyncTaskRepository.save(entity);
    }

    Optional<GenerationAsyncTaskEntity> findReplayTask(
            BatchGenerateRequestBody request,
            UUID templateId,
            String requestHash
    ) {
        return asyncTaskRepository.findByIdempotencyKeyAndTemplateId(request.idempotencyKey(), templateId)
                .filter(task -> task.getRequestHash().equals(requestHash))
                .filter(task -> task.getExpiresAt().isAfter(Instant.now()));
    }

    GenerationAsyncTaskEntity createAcceptedTask(
            TemplateEntity template,
            String routeType,
            String resolvedVersion,
            BatchGenerateRequestBody request,
            String requestHash,
            AsyncBatchTaskDispatcher dispatcher
    ) {
        String taskId = "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        String batchId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        GenerationAsyncTaskEntity task = new GenerationAsyncTaskEntity(
                UUID.randomUUID(),
                taskId,
                batchId,
                template.getId(),
                TaskStatus.ACCEPTED,
                routeType,
                resolvedVersion,
                request.requestId(),
                request.idempotencyKey(),
                requestHash,
                json.writeRequestPayload(request),
                Instant.now().plusSeconds(IdempotencyConstants.RETENTION_SECONDS)
        );
        asyncTaskRepository.save(task);
        dispatcher.dispatch(task.getId());
        return task;
    }

    Optional<BatchGenerateResultView> resolveSyncReplay(Optional<GenerationAsyncTaskEntity> existing) {
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        GenerationAsyncTaskEntity replay = existing.get();
        if (replay.getStatus() == TaskStatus.FAILED && replay.getBatchResultJson() != null) {
            throw new SyncBatchFailureException(json.readBatchResult(replay.getBatchResultJson()));
        }
        if (replay.getBatchResultJson() != null) {
            return Optional.of(new BatchGenerateResultView(json.readBatchResult(replay.getBatchResultJson())));
        }
        return Optional.empty();
    }

    TaskSummaryView toTaskSummary(
            GenerationAsyncTaskEntity task,
            String templateExternalId,
            String environment
    ) {
        String queryPath = task.getTaskExternalId() == null ? null
                : "/api/" + environment + "/v1/templates/" + templateExternalId + "/tasks/" + task.getTaskExternalId();
        return new TaskSummaryView(
                task.getTaskExternalId(),
                task.getStatus().name(),
                queryPath,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getExpiresAt()
        );
    }
}
