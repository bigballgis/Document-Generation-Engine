package com.bank.docgen.infrastructure.config;

import com.bank.docgen.runtime.messaging.AsyncBatchTaskMessage;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "docgen.async.transport", havingValue = "kafka")
public class KafkaAsyncBatchConfig {

    @Bean
    public ProducerFactory<String, AsyncBatchTaskMessage> asyncBatchTaskProducerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, AsyncBatchTaskMessage> asyncBatchTaskKafkaTemplate(
            ProducerFactory<String, AsyncBatchTaskMessage> asyncBatchTaskProducerFactory
    ) {
        return new KafkaTemplate<>(asyncBatchTaskProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, AsyncBatchTaskMessage> asyncBatchTaskConsumerFactory(
            KafkaProperties kafkaProperties,
            DocgenAsyncProperties asyncProperties
    ) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, asyncProperties.getKafka().getConsumerGroup());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.bank.docgen.runtime.messaging");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AsyncBatchTaskMessage.class.getName());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AsyncBatchTaskMessage> asyncBatchTaskKafkaListenerContainerFactory(
            ConsumerFactory<String, AsyncBatchTaskMessage> asyncBatchTaskConsumerFactory,
            KafkaTemplate<String, AsyncBatchTaskMessage> asyncBatchTaskKafkaTemplate,
            DocgenAsyncProperties asyncProperties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, AsyncBatchTaskMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(asyncBatchTaskConsumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                asyncBatchTaskKafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        asyncProperties.getKafka().getDeadLetterTopic(),
                        record.partition()
                )
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
