package com.bank.docgen.sharedkernel.health;

import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class KafkaReadinessContributor implements ComponentReadinessContributor {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final DocgenAsyncProperties asyncProperties;
    private final KafkaProperties kafkaProperties;

    public KafkaReadinessContributor(DocgenAsyncProperties asyncProperties, KafkaProperties kafkaProperties) {
        this.asyncProperties = asyncProperties;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public String componentName() {
        return "kafka";
    }

    @Override
    public ComponentCheck check() {
        if (!"kafka".equalsIgnoreCase(asyncProperties.getTransport())) {
            return new ComponentCheck("SKIPPED", "async transport is not kafka");
        }
        Properties config = new Properties();
        config.putAll(kafkaProperties.buildAdminProperties(null));
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(TIMEOUT.toMillis()));
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, String.valueOf(TIMEOUT.toMillis()));
        try (AdminClient adminClient = AdminClient.create(config)) {
            adminClient.describeCluster()
                    .clusterId()
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return new ComponentCheck("UP", null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new ComponentCheck("DOWN", null);
        } catch (ExecutionException | TimeoutException | RuntimeException ex) {
            return new ComponentCheck("DOWN", null);
        }
    }
}
