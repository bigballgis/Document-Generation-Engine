package com.bank.docgen.runtime.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.service.AsyncBatchTaskRunner;
import com.bank.docgen.sharedkernel.api.TraceIdConstants;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

/**
 * LR-D4 Scenario B (consumer half): header {@code X-Trace-Id} restores MDC before processTask.
 */
@ExtendWith(MockitoExtension.class)
class AsyncBatchTaskKafkaConsumerTracePropagationTest {

    @Mock
    private AsyncBatchTaskRunner asyncBatchTaskRunner;

    private AsyncBatchTaskKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.getKafka().setAsyncBatchTopic("generation.async-batch-task.v1");
        consumer = new AsyncBatchTaskKafkaConsumer(asyncBatchTaskRunner, properties);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void scenarioB_consumerRestoresTraceIdFromHeaderIntoMdc() {
        UUID taskId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        String producerTraceId = "trace-kafka-round-trip-b";
        AtomicReference<String> mdcDuringProcess = new AtomicReference<>();

        doAnswer(invocation -> {
            mdcDuringProcess.set(MDC.get(TraceIdConstants.MDC_KEY));
            return null;
        }).when(asyncBatchTaskRunner).processTask(any(UUID.class));

        ConsumerRecord<String, AsyncBatchTaskMessage> record = new ConsumerRecord<>(
                "generation.async-batch-task.v1",
                0,
                0L,
                taskId.toString(),
                new AsyncBatchTaskMessage(taskId.toString())
        );
        record.headers().add(new RecordHeader(
                TraceIdConstants.HEADER_NAME,
                producerTraceId.getBytes(StandardCharsets.UTF_8)
        ));

        consumer.consume(record);

        verify(asyncBatchTaskRunner).processTask(taskId);
        assertThat(mdcDuringProcess.get()).isEqualTo(producerTraceId);
        assertThat(MDC.get(TraceIdConstants.MDC_KEY)).isNull();
    }

    @Test
    void consumeWithoutTraceHeaderStillProcessesTask() {
        UUID taskId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        AtomicReference<String> mdcDuringProcess = new AtomicReference<>("unset");

        doAnswer(invocation -> {
            mdcDuringProcess.set(MDC.get(TraceIdConstants.MDC_KEY));
            return null;
        }).when(asyncBatchTaskRunner).processTask(any(UUID.class));

        ConsumerRecord<String, AsyncBatchTaskMessage> record = new ConsumerRecord<>(
                "generation.async-batch-task.v1",
                0,
                0L,
                taskId.toString(),
                new AsyncBatchTaskMessage(taskId.toString())
        );

        consumer.consume(record);

        verify(asyncBatchTaskRunner).processTask(taskId);
        assertThat(mdcDuringProcess.get()).isNull();
    }
}
