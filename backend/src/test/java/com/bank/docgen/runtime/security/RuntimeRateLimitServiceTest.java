package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import org.junit.jupiter.api.Test;

class RuntimeRateLimitServiceTest {

    @Test
    void allowsBurstThenRejectsAdditionalRequests() {
        // Low refill rate so greedy refill cannot restore a token during the assertion window.
        RuntimeRateLimitService service = new RuntimeRateLimitService(
                new RuntimeRateLimitProperties(true, 1, 3, false)
        );

        assertThat(service.tryConsume("CRED-1", "svc-a").isAllowed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isAllowed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isAllowed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isDenied()).isTrue();
    }

    @Test
    void isolatesBucketsPerCredentialAndAccount() {
        RuntimeRateLimitService service = new RuntimeRateLimitService(
                new RuntimeRateLimitProperties(true, 60, 1, false)
        );

        assertThat(service.tryConsume("CRED-1", "svc-a").isAllowed()).isTrue();
        assertThat(service.tryConsume("CRED-1", "svc-a").isDenied()).isTrue();
        assertThat(service.tryConsume("CRED-2", "svc-a").isAllowed()).isTrue();
    }

    @Test
    void distributedDefaultsFalse() {
        RuntimeRateLimitProperties properties = new RuntimeRateLimitProperties(true, 60, 3, false);
        assertThat(properties.distributed()).isFalse();
        assertThat(new RuntimeRateLimitService(properties).enabled()).isTrue();
    }
}
