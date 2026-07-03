package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionAsyncTransportGuardTest {

    @Test
    void productionEnvironmentRejectsInProcessAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");
        environment.setActiveProfiles("prod");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kafka");
    }

    @Test
    void productionEnvironmentAllowsKafkaAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");
        environment.setActiveProfiles("prod");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("kafka");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        guard.verifyOrThrow();
    }
}
