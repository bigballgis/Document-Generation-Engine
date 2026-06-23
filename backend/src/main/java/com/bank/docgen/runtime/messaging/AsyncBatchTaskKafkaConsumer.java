package com.bank.docgen.runtime.messaging;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.service.AsyncBatchTaskRunner;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docgen.async.transport", havingValue = "kafka")
public class AsyncBatchTaskKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncBatchTaskKafkaConsumer.class);

    private final AsyncBatchTaskRunner asyncBatchTaskRunner;
    private final DocgenAsyncProperties asyncProperties;

    public AsyncBatchTaskKafkaConsumer(
            AsyncBatchTaskRunner asyncBatchTaskRunner,
            DocgenAsyncProperties asyncProperties
    ) {
        this.asyncBatchTaskRunner = asyncBatchTaskRunner;
        this.asyncProperties = asyncProperties;
    }

    @KafkaListener(
            topics = "${docgen.async.kafka.async-batch-topic}",
            groupId = "${docgen.async.kafka.consumer-group}",
            containerFactory = "asyncBatchTaskKafkaListenerContainerFactory"
    )
    public void consume(AsyncBatchTaskMessage message) {
        LOGGER.debug(
                "Received async batch task message on topic {}",
                asyncProperties.getKafka().getAsyncBatchTopic()
        );
        asyncBatchTaskRunner.processTask(UUID.fromString(message.taskId()));
    }
}
