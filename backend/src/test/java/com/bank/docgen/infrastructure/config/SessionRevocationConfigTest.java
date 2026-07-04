package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bank.docgen.authorization.management.session.InMemorySessionRevocationStore;
import com.bank.docgen.authorization.management.session.RedisSessionRevocationStore;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class SessionRevocationConfigTest {

    private final SessionRevocationConfig config = new SessionRevocationConfig();

    @Test
    void redisStoreIsTheNonTestDefault() {
        assertThat(config.redisSessionRevocationStore(mock(StringRedisTemplate.class)))
                .isInstanceOf(RedisSessionRevocationStore.class);
    }

    @Test
    void memoryStoreIsAllowedOutsideProd() {
        assertThat(config.memorySessionRevocationStore("dev"))
                .isInstanceOf(InMemorySessionRevocationStore.class);
        assertThat(config.testSessionRevocationStore())
                .isInstanceOf(InMemorySessionRevocationStore.class);
    }

    @Test
    void memoryStoreRefusesToStartInProd() {
        // Spec B7 guard: revocation must survive instance restarts in prod.
        assertThatThrownBy(() -> config.memorySessionRevocationStore("prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forbidden");
    }
}
