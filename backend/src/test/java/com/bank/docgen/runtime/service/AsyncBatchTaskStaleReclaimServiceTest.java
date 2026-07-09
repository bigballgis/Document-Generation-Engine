package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncBatchTaskStaleReclaimServiceTest {

    private static final UUID TASK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private GenerationAsyncTaskRepository asyncTaskRepository;
    @Mock
    private AsyncBatchTaskDispatcher asyncBatchTaskDispatcher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DocgenAsyncProperties asyncProperties;
    private AsyncBatchTaskStaleReclaimService reclaimService;

    @BeforeEach
    void setUp() {
        asyncProperties = new DocgenAsyncProperties();
        asyncProperties.setStaleProcessingThresholdSeconds(900L);
        reclaimService = new AsyncBatchTaskStaleReclaimService(
                asyncTaskRepository,
                asyncBatchTaskDispatcher,
                asyncProperties,
                new AsyncBatchPayloadScrubber(objectMapper),
                objectMapper
        );
    }

    @Test
    void reclaimResetsToAcceptedAndRedispatchesWhenAttemptsRemain() throws Exception {
        GenerationAsyncTaskEntity task = staleProcessingTask(1);
        when(asyncTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(asyncTaskRepository.compareAndSetStatus(
                eq(TASK_ID),
                eq(TaskStatus.ACCEPTED),
                any(Instant.class),
                eq(TaskStatus.PROCESSING),
                eq(task.getUpdatedAt())
        )).thenReturn(1);

        assertThat(reclaimService.reclaimTask(TASK_ID)).isTrue();

        verify(asyncBatchTaskDispatcher).dispatch(TASK_ID);
    }

    @Test
    void reclaimMarksFailedWhenAttemptsExhaustedAndScrubsPayload() throws Exception {
        GenerationAsyncTaskEntity task = staleProcessingTask(3);
        when(asyncTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(asyncTaskRepository.compareAndSetStatus(
                eq(TASK_ID),
                eq(TaskStatus.FAILED),
                any(Instant.class),
                eq(TaskStatus.PROCESSING),
                eq(task.getUpdatedAt())
        )).thenReturn(1);
        when(asyncTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task), Optional.of(task));

        assertThat(reclaimService.reclaimTask(TASK_ID)).isTrue();

        ArgumentCaptor<GenerationAsyncTaskEntity> captor = ArgumentCaptor.forClass(GenerationAsyncTaskEntity.class);
        verify(asyncTaskRepository).save(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getRequestPayloadJson());
        assertThat(payload.toString()).doesNotContain("1234567890");
        assertThat(payload.path("items").get(0).has("variablesHash")).isTrue();
        verify(asyncBatchTaskDispatcher, never()).dispatch(any());
    }

    private GenerationAsyncTaskEntity staleProcessingTask(int attempts) throws Exception {
        BatchGenerateRequestBody request = new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody(
                        "item-1",
                        Map.of("accountNo", "1234567890"),
                        null,
                        null
                )),
                null,
                "req-1",
                "idem-1"
        );
        GenerationAsyncTaskEntity task = new GenerationAsyncTaskEntity(
                TASK_ID,
                "TASK-1",
                "BATCH-1",
                UUID.randomUUID(),
                TaskStatus.ACCEPTED,
                "DEFAULT",
                "1.0.0",
                "req-1",
                "idem-1",
                "hash",
                objectMapper.writeValueAsString(request),
                Instant.now().plusSeconds(3600)
        );
        for (int i = 0; i < attempts; i++) {
            task.markProcessing();
        }
        var updatedAtField = GenerationAsyncTaskEntity.class.getDeclaredField("updatedAt");
        updatedAtField.setAccessible(true);
        updatedAtField.set(task, Instant.now().minusSeconds(3600));
        return task;
    }
}
