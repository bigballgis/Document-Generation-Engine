package com.bank.docgen.runtime.service;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.runtime.messaging.AsyncBatchTaskMessage;
import com.bank.docgen.sharedkernel.api.TraceIdConstants;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docgen.async.transport", havingValue = "kafka")
public class KafkaAsyncBatchTaskDispatcher implements AsyncBatchTaskDispatcher {

    private final KafkaTemplate<String, AsyncBatchTaskMessage> kafkaTemplate;
    private final DocgenAsyncProperties asyncProperties;
    private final TraceIdProvider traceIdProvider;

    public KafkaAsyncBatchTaskDispatcher(
            KafkaTemplate<String, AsyncBatchTaskMessage> kafkaTemplate,
            DocgenAsyncProperties asyncProperties,
            TraceIdProvider traceIdProvider
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.asyncProperties = asyncProperties;
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    public void dispatch(UUID taskId) {
        AsyncBatchTaskMessage message = new AsyncBatchTaskMessage(taskId.toString());
        String topic = asyncProperties.getKafka().getAsyncBatchTopic();
        ProducerRecord<String, AsyncBatchTaskMessage> record =
                new ProducerRecord<>(topic, taskId.toString(), message);
        record.headers().add(new RecordHeader(
                TraceIdConstants.HEADER_NAME,
                resolveTraceId().getBytes(StandardCharsets.UTF_8)
        ));
        kafkaTemplate.send(record);
    }

    private String resolveTraceId() {
        String fromMdc = MDC.get(TraceIdConstants.MDC_KEY);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        return traceIdProvider.currentOrNew(null);
    }
}
