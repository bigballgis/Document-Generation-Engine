package com.bank.docgen.infrastructure.config;

import com.bank.docgen.runtime.metrics.AsyncDltDepthMetrics;
import com.bank.docgen.runtime.metrics.KafkaAsyncDltDepthProbe;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Registers async DLT depth gauge when Kafka transport is active (LR-D3).
 */
@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "docgen.async.transport", havingValue = "kafka")
public class AsyncDltMetricsConfig {

    @Bean
    public KafkaAsyncDltDepthProbe kafkaAsyncDltDepthProbe(
            DocgenAsyncProperties asyncProperties,
            KafkaProperties kafkaProperties
    ) {
        return new KafkaAsyncDltDepthProbe(asyncProperties, kafkaProperties);
    }

    @Bean
    public AsyncDltDepthMetrics asyncDltDepthMetrics(
            KafkaAsyncDltDepthProbe kafkaAsyncDltDepthProbe,
            MeterRegistry meterRegistry
    ) {
        AsyncDltDepthMetrics metrics = new AsyncDltDepthMetrics(kafkaAsyncDltDepthProbe);
        metrics.bindTo(meterRegistry);
        return metrics;
    }
}
