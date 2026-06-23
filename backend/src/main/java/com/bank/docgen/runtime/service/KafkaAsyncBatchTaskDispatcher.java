package com.bank.docgen.runtime.service;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.messaging.AsyncBatchTaskMessage;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docgen.async.transport", havingValue = "kafka")
public class KafkaAsyncBatchTaskDispatcher implements AsyncBatchTaskDispatcher {

    private final KafkaTemplate<String, AsyncBatchTaskMessage> kafkaTemplate;
    private final DocgenAsyncProperties asyncProperties;

    public KafkaAsyncBatchTaskDispatcher(
            KafkaTemplate<String, AsyncBatchTaskMessage> kafkaTemplate,
            DocgenAsyncProperties asyncProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.asyncProperties = asyncProperties;
    }

    @Override
    public void dispatch(UUID taskId) {
        AsyncBatchTaskMessage message = new AsyncBatchTaskMessage(taskId.toString());
        kafkaTemplate.send(asyncProperties.getKafka().getAsyncBatchTopic(), taskId.toString(), message);
    }
}
