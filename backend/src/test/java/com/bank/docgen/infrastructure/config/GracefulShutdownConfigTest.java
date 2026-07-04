package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.runtime.messaging.AsyncBatchTaskMessage;
import java.util.Properties;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * LR-B5: graceful shutdown and drain configuration. The lifecycle phase timeout is 30s
 * (env-overridable); executors and the Kafka listener container must drain within ~25s
 * so they finish below the phase timeout.
 */
class GracefulShutdownConfigTest {

    private static final long EXPECTED_AWAIT_TERMINATION_MILLIS = 25_000L;

    @Test
    void applicationYamlEnablesGracefulShutdownWithBoundedDrainTimeout() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
                .isEqualTo("${SHUTDOWN_DRAIN_TIMEOUT:30s}");
    }

    @Test
    void asyncTaskExecutorWaitsForTasksAndDrainsBelowPhaseTimeout() {
        Executor executor = new AsyncConfig().asyncTaskExecutor();
        try {
            assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
            assertThat(ReflectionTestUtils.getField(executor, "waitForTasksToCompleteOnShutdown"))
                    .isEqualTo(true);
            assertThat(ReflectionTestUtils.getField(executor, "awaitTerminationMillis"))
                    .isEqualTo(EXPECTED_AWAIT_TERMINATION_MILLIS);
        } finally {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    }

    @Test
    void pdfConversionExecutorWaitsForTasksAndDrainsBelowPhaseTimeout() {
        DocgenRenderingProperties renderingProperties = new DocgenRenderingProperties();
        ThreadPoolTaskExecutor executor =
                new PdfConversionExecutorConfig().pdfConversionExecutor(renderingProperties);
        try {
            assertThat(ReflectionTestUtils.getField(executor, "waitForTasksToCompleteOnShutdown"))
                    .isEqualTo(true);
            assertThat(ReflectionTestUtils.getField(executor, "awaitTerminationMillis"))
                    .isEqualTo(EXPECTED_AWAIT_TERMINATION_MILLIS);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void kafkaListenerContainerShutdownTimeoutStaysBelowPhaseTimeout() {
        KafkaAsyncBatchConfig config = new KafkaAsyncBatchConfig();
        KafkaProperties kafkaProperties = new KafkaProperties();
        DocgenAsyncProperties asyncProperties = new DocgenAsyncProperties();
        ProducerFactory<String, AsyncBatchTaskMessage> producerFactory =
                config.asyncBatchTaskProducerFactory(kafkaProperties);
        KafkaTemplate<String, AsyncBatchTaskMessage> template =
                config.asyncBatchTaskKafkaTemplate(producerFactory);
        ConsumerFactory<String, AsyncBatchTaskMessage> consumerFactory =
                config.asyncBatchTaskConsumerFactory(kafkaProperties, asyncProperties);

        ConcurrentKafkaListenerContainerFactory<String, AsyncBatchTaskMessage> factory =
                config.asyncBatchTaskKafkaListenerContainerFactory(consumerFactory, template, asyncProperties);

        assertThat(factory.getContainerProperties().getShutdownTimeout())
                .isEqualTo(EXPECTED_AWAIT_TERMINATION_MILLIS);
    }
}
