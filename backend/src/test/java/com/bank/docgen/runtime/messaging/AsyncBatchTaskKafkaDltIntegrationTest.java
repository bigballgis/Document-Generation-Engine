package com.bank.docgen.runtime.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.bank.docgen.runtime.service.AsyncBatchTaskRunner;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "test-kafka"})
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "generation.async-batch-task.v1",
                "generation.async-batch-task.v1.dlt"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AsyncBatchTaskKafkaDltIntegrationTest {

    @Autowired
    private KafkaTemplate<String, AsyncBatchTaskMessage> asyncBatchTaskKafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private AsyncBatchTaskRunner asyncBatchTaskRunner;

    @Test
    void failedProcessingPublishesToDltAfterRetryExhaustion() throws Exception {
        UUID taskId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        doThrow(new RuntimeException("simulated worker failure"))
                .when(asyncBatchTaskRunner)
                .processTask(any());

        asyncBatchTaskKafkaTemplate
                .send("generation.async-batch-task.v1", taskId.toString(), new AsyncBatchTaskMessage(taskId.toString()))
                .get(5, TimeUnit.SECONDS);

        try (Consumer<String, AsyncBatchTaskMessage> dltConsumer = createDltConsumer()) {
            var records = KafkaTestUtils.getRecords(dltConsumer, Duration.ofSeconds(30));
            assertThat(records.count()).isEqualTo(1);
            ConsumerRecord<String, AsyncBatchTaskMessage> record = records.iterator().next();
            assertThat(record.topic()).isEqualTo("generation.async-batch-task.v1.dlt");
            assertThat(record.value().taskId()).isEqualTo(taskId.toString());
        }
    }

    private Consumer<String, AsyncBatchTaskMessage> createDltConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "dlt-test-group",
                "true",
                embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.bank.docgen.runtime.messaging");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AsyncBatchTaskMessage.class.getName());
        DefaultKafkaConsumerFactory<String, AsyncBatchTaskMessage> factory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, AsyncBatchTaskMessage> consumer = factory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "generation.async-batch-task.v1.dlt");
        return consumer;
    }
}
