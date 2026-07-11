package com.bank.docgen.runtime.messaging;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.service.AsyncBatchTaskRunner;
import com.bank.docgen.sharedkernel.api.TraceIdConstants;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    public void consume(ConsumerRecord<String, AsyncBatchTaskMessage> record) {
        String traceId = extractTraceId(record);
        if (traceId != null) {
            MDC.put(TraceIdConstants.MDC_KEY, traceId);
        }
        try {
            LOGGER.debug(
                    "Received async batch task message on topic {}",
                    asyncProperties.getKafka().getAsyncBatchTopic()
            );
            AsyncBatchTaskMessage message = record.value();
            asyncBatchTaskRunner.processTask(UUID.fromString(message.taskId()));
        } finally {
            MDC.remove(TraceIdConstants.MDC_KEY);
        }
    }

    private static String extractTraceId(ConsumerRecord<String, AsyncBatchTaskMessage> record) {
        Header header = record.headers().lastHeader(TraceIdConstants.HEADER_NAME);
        if (header == null || header.value() == null || header.value().length == 0) {
            return null;
        }
        String value = new String(header.value(), StandardCharsets.UTF_8).trim();
        return value.isEmpty() ? null : value;
    }
}
