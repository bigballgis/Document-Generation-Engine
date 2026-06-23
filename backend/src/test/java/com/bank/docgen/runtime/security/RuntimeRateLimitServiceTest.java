package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import org.junit.jupiter.api.Test;

class RuntimeRateLimitServiceTest {

    @Test
    void allowsBurstThenRejectsAdditionalRequests() {
        RuntimeRateLimitService service = new RuntimeRateLimitService(
                new RuntimeRateLimitProperties(true, 60, 3)
        );

        assertThat(service.tryConsume("CRED-1", "svc-a").isConsumed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isConsumed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isConsumed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isConsumed()).isFalse();
    }

    @Test
    void isolatesBucketsPerCredentialAndAccount() {
        RuntimeRateLimitService service = new RuntimeRateLimitService(
                new RuntimeRateLimitProperties(true, 60, 1)
        );

        assertThat(service.tryConsume("CRED-1", "svc-a").isConsumed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isConsumed()).isFalse();
        assertThat(service.tryConsume("CRED-2", "svc-a").isConsumed()).isTrue();
    }
}
