package com.bank.docgen.runtime.metrics;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

/**
 * Polls Kafka AdminClient for approximate DLT topic end-offset sum (message depth proxy).
 * Failures retain the last successful reading; initial value is 0.
 */
public class KafkaAsyncDltDepthProbe implements LongSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaAsyncDltDepthProbe.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final DocgenAsyncProperties asyncProperties;
    private final KafkaProperties kafkaProperties;
    private final AtomicLong lastDepth = new AtomicLong(0);

    public KafkaAsyncDltDepthProbe(DocgenAsyncProperties asyncProperties, KafkaProperties kafkaProperties) {
        this.asyncProperties = asyncProperties;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public long getAsLong() {
        try {
            long depth = queryDepth();
            lastDepth.set(depth);
            return depth;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.debug("DLT depth probe interrupted; returning last value {}", lastDepth.get());
            return lastDepth.get();
        } catch (Exception ex) {
            LOG.debug("DLT depth probe failed; returning last value {}: {}", lastDepth.get(), ex.toString());
            return lastDepth.get();
        }
    }

    private long queryDepth() throws Exception {
        String topic = asyncProperties.getKafka().getDeadLetterTopic();
        Properties config = new Properties();
        config.putAll(kafkaProperties.buildAdminProperties(null));
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(TIMEOUT.toMillis()));
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, String.valueOf(TIMEOUT.toMillis()));
        try (AdminClient adminClient = AdminClient.create(config)) {
            Collection<org.apache.kafka.common.TopicPartitionInfo> partitions = adminClient
                    .describeTopics(java.util.List.of(topic))
                    .allTopicNames()
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .get(topic)
                    .partitions();
            Map<TopicPartition, OffsetSpec> latestSpecs = new HashMap<>();
            Map<TopicPartition, OffsetSpec> earliestSpecs = new HashMap<>();
            for (org.apache.kafka.common.TopicPartitionInfo partition : partitions) {
                TopicPartition tp = new TopicPartition(topic, partition.partition());
                latestSpecs.put(tp, OffsetSpec.latest());
                earliestSpecs.put(tp, OffsetSpec.earliest());
            }
            ListOffsetsResult latest = adminClient.listOffsets(latestSpecs);
            ListOffsetsResult earliest = adminClient.listOffsets(earliestSpecs);
            long depth = 0L;
            for (TopicPartition tp : latestSpecs.keySet()) {
                long end = latest.partitionResult(tp).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).offset();
                long start = earliest.partitionResult(tp).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).offset();
                depth += Math.max(0L, end - start);
            }
            return depth;
        }
    }
}
