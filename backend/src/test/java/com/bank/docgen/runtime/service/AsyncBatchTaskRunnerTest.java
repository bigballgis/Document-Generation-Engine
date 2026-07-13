package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateValidationException;
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
class AsyncBatchTaskRunnerTest {

    private static final UUID TASK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private GenerationAsyncTaskRepository asyncTaskRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private BatchExecutionService batchExecutionService;
    @Mock
    private RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private InvocationRecordService invocationRecordService;
    @Mock
    private AsyncBatchTaskStaleReclaimService staleReclaimService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AsyncBatchTaskRunner runner;
    private AsyncBatchPayloadScrubber payloadScrubber;

    @BeforeEach
    void setUp() {
        payloadScrubber = new AsyncBatchPayloadScrubber(objectMapper);
        runner = new AsyncBatchTaskRunner(
                asyncTaskRepository,
                templateRepository,
                batchExecutionService,
                runtimeGenerationAuditRecorder,
                objectMapper,
                apiPolicyRepository,
                invocationRecordService,
                new TraceIdProvider(),
                staleReclaimService,
                payloadScrubber
        );
    }

    @Test
    void processingWithinLeaseSkipsExecution() {
        GenerationAsyncTaskEntity task = processingTask(Instant.now());
        when(asyncTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(staleReclaimService.isStale(task)).thenReturn(false);

        runner.processTask(TASK_ID);

        verify(batchExecutionService, never()).execute(any(), any(), any(), any(), eq(true));
        verify(asyncTaskRepository, never()).save(any());
    }

    @Test
    void staleProcessingDelegatesToReclaimService() {
        GenerationAsyncTaskEntity task = processingTask(Instant.now().minusSeconds(3600));
        when(asyncTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(staleReclaimService.isStale(task)).thenReturn(true);

        runner.processTask(TASK_ID);

        verify(staleReclaimService).reclaimTask(TASK_ID);
        verify(batchExecutionService, never()).execute(any(), any(), any(), any(), eq(true));
    }

    @Test
    void terminalSucceededSkipsExecution() {
        GenerationAsyncTaskEntity task = acceptedTask();
        task.markSucceeded("{}");
        when(asyncTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        runner.processTask(TASK_ID);

        verify(batchExecutionService, never()).execute(any(), any(), any(), any(), eq(true));
    }

    @Test
    void successScrubsSensitivePayload() throws Exception {
        BatchGenerateRequestBody request = batchRequest(Map.of("accountNo", "1234567890"));
        GenerationAsyncTaskEntity task = acceptedTask();
        task = spyTaskWithPayload(task, request);
        TemplateEntity template = templateEntity();

        when(asyncTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(batchExecutionService.execute(any(), any(), any(), any(), eq(true)))
                .thenReturn(new BatchExecutionService.BatchExecutionOutcome(
                        new BatchResultView("BATCH-1", new BatchSummaryView(1, 1, 0, 0, 0), List.of()),
                        TaskStatus.SUCCEEDED
                ));
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.empty());

        runner.processTask(TASK_ID);

        ArgumentCaptor<GenerationAsyncTaskEntity> captor = ArgumentCaptor.forClass(GenerationAsyncTaskEntity.class);
        verify(asyncTaskRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getRequestPayloadJson());
        assertThat(payload.toString()).doesNotContain("1234567890");
        assertThat(payload.path("items").get(0).has("variablesHash")).isTrue();
    }

    @Test
    void failureSummaryUsesMessageKeyNotSensitiveText() {
        TemplateValidationException ex = new TemplateValidationException("api.error.validation.requestBodyInvalid");
        assertThat(AsyncBatchTaskRunner.summarizeFailure(ex))
                .isEqualTo("api.error.validation.requestBodyInvalid");
    }

    @Test
    void failureSummaryUsesExceptionClassNameForGenericFailures() {
        RuntimeException ex = new RuntimeException("accountNo=1234567890");
        assertThat(AsyncBatchTaskRunner.summarizeFailure(ex)).isEqualTo("RuntimeException");
    }

    @Test
    void isTerminalStatusExcludesProcessing() {
        assertThat(AsyncBatchTaskRunner.isTerminalStatus(TaskStatus.PROCESSING)).isFalse();
        assertThat(AsyncBatchTaskRunner.isTerminalStatus(TaskStatus.SUCCEEDED)).isTrue();
        assertThat(AsyncBatchTaskRunner.isTerminalStatus(TaskStatus.FAILED)).isTrue();
    }

    private GenerationAsyncTaskEntity acceptedTask() {
        return new GenerationAsyncTaskEntity(
                TASK_ID,
                "TASK-1",
                "BATCH-1",
                TEMPLATE_ID,
                TaskStatus.ACCEPTED,
                "DEFAULT",
                "1.0.0",
                "req-1",
                "idem-1",
                "hash",
                "{}",
                Instant.now().plusSeconds(3600)
        );
    }

    private GenerationAsyncTaskEntity processingTask(Instant updatedAt) {
        GenerationAsyncTaskEntity task = acceptedTask();
        task.markProcessing();
        try {
            var field = GenerationAsyncTaskEntity.class.getDeclaredField("updatedAt");
            field.setAccessible(true);
            field.set(task, updatedAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return task;
    }

    private GenerationAsyncTaskEntity spyTaskWithPayload(
            GenerationAsyncTaskEntity task,
            BatchGenerateRequestBody request
    ) throws Exception {
        String json = objectMapper.writeValueAsString(request);
        var field = GenerationAsyncTaskEntity.class.getDeclaredField("requestPayloadJson");
        field.setAccessible(true);
        field.set(task, json);
        return task;
    }

    private BatchGenerateRequestBody batchRequest(Map<String, Object> variables) {
        return new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody("item-1", variables, null, null)),
                null,
                "req-1",
                "idem-1",
                null,
                null
        );
    }

    private TemplateEntity templateEntity() {
        return new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
    }
}
