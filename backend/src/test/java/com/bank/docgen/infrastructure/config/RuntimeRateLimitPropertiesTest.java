package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuntimeRateLimitPropertiesTest {

    @Test
    void distributedDefaultsFalse() {
        RuntimeRateLimitProperties properties = new RuntimeRateLimitProperties(true, 120, 120, false);
        assertThat(properties.distributed()).isFalse();
        assertThat(properties.enabled()).isTrue();
    }

    @Test
    void distributedCanBeEnabled() {
        RuntimeRateLimitProperties properties = new RuntimeRateLimitProperties(true, 120, 120, true);
        assertThat(properties.distributed()).isTrue();
    }
}
