package com.bank.docgen.runtime.service;

import static org.mockito.Mockito.verify;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.messaging.AsyncBatchTaskMessage;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        dispatcher = new KafkaAsyncBatchTaskDispatcher(kafkaTemplate, properties);
    }

    @Test
    void dispatchPublishesTaskIdToConfiguredTopic() {
        UUID taskId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        dispatcher.dispatch(taskId);

        ArgumentCaptor<AsyncBatchTaskMessage> messageCaptor = ArgumentCaptor.forClass(AsyncBatchTaskMessage.class);
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("generation.async-batch-task.v1"),
                org.mockito.ArgumentMatchers.eq(taskId.toString()),
                messageCaptor.capture()
        );
        org.junit.jupiter.api.Assertions.assertEquals(taskId.toString(), messageCaptor.getValue().taskId());
    }
}
