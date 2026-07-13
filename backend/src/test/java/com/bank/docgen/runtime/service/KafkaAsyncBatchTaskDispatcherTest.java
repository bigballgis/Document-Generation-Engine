package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.messaging.AsyncBatchTaskMessage;
import com.bank.docgen.sharedkernel.api.TraceIdConstants;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaAsyncBatchTaskDispatcherTest {

    @Mock
    private KafkaTemplate<String, AsyncBatchTaskMessage> kafkaTemplate;

    private KafkaAsyncBatchTaskDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.getKafka().setAsyncBatchTopic("generation.async-batch-task.v1");
        dispatcher = new KafkaAsyncBatchTaskDispatcher(kafkaTemplate, properties, new TraceIdProvider());
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void dispatchPublishesTaskIdToConfiguredTopic() {
        UUID taskId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        dispatcher.dispatch(taskId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, AsyncBatchTaskMessage>> recordCaptor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());

        ProducerRecord<String, AsyncBatchTaskMessage> record = recordCaptor.getValue();
        assertThat(record.topic()).isEqualTo("generation.async-batch-task.v1");
        assertThat(record.key()).isEqualTo(taskId.toString());
        assertThat(record.value().taskId()).isEqualTo(taskId.toString());
        assertThat(record.headers().lastHeader(TraceIdConstants.HEADER_NAME)).isNotNull();
    }

    @Test
    void scenarioB_dispatchAttachesXTraceIdHeaderFromMdc() {
        UUID taskId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        String producerTraceId = "trace-kafka-round-trip-b";
        MDC.put(TraceIdConstants.MDC_KEY, producerTraceId);

        dispatcher.dispatch(taskId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, AsyncBatchTaskMessage>> recordCaptor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());

        byte[] headerValue = recordCaptor.getValue().headers().lastHeader(TraceIdConstants.HEADER_NAME).value();
        assertThat(new String(headerValue, StandardCharsets.UTF_8)).isEqualTo(producerTraceId);
        assertThat(recordCaptor.getValue().value().taskId()).isEqualTo(taskId.toString());
    }
}
