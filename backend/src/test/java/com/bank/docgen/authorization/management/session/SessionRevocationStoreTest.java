package com.bank.docgen.authorization.management.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SessionRevocationStoreTest {

    private static final String KEY_PREFIX = "docgen:session:revoked:";

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class RedisStore {

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        @Test
        void revokeStoresJtiWithRemainingTokenLifeTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            RedisSessionRevocationStore store = new RedisSessionRevocationStore(redisTemplate);
            Instant expiresAt = Instant.now().plusSeconds(1200);

            store.revoke("jti-J1", expiresAt);

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOperations).set(eq(KEY_PREFIX + "jti-J1"), anyString(), ttlCaptor.capture());
            assertThat(ttlCaptor.getValue().toSeconds()).isBetween(1100L, 1200L);
        }

        @Test
        void revokeSkipsTokenAlreadyPastItsExpiry() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            RedisSessionRevocationStore store = new RedisSessionRevocationStore(redisTemplate);

            store.revoke("jti-J2", Instant.now().minusSeconds(5));

            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        void isRevokedReflectsRevocationListMembership() {
            RedisSessionRevocationStore store = new RedisSessionRevocationStore(redisTemplate);
            when(redisTemplate.hasKey(KEY_PREFIX + "jti-J3")).thenReturn(Boolean.TRUE);
            when(redisTemplate.hasKey(KEY_PREFIX + "jti-J4")).thenReturn(Boolean.FALSE);

            assertThat(store.isRevoked("jti-J3")).isTrue();
            assertThat(store.isRevoked("jti-J4")).isFalse();
        }

        @Test
        void readFailureSurfacesAsUnavailableForFailClosedHandling() {
            RedisSessionRevocationStore store = new RedisSessionRevocationStore(redisTemplate);
            when(redisTemplate.hasKey(anyString()))
                    .thenThrow(new RedisConnectionFailureException("connection refused"));

            assertThatThrownBy(() -> store.isRevoked("jti-J5"))
                    .isInstanceOf(SessionRevocationUnavailableException.class);
        }

        @Test
        void writeFailureSurfacesAsUnavailable() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            RedisSessionRevocationStore store = new RedisSessionRevocationStore(redisTemplate);
            org.mockito.Mockito.doThrow(new RedisConnectionFailureException("connection refused"))
                    .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            assertThatThrownBy(() -> store.revoke("jti-J6", Instant.now().plusSeconds(600)))
                    .isInstanceOf(SessionRevocationUnavailableException.class);
        }
    }

    @Nested
    class InMemoryStore {

        private final InMemorySessionRevocationStore store = new InMemorySessionRevocationStore();

        @Test
        void revokedJtiIsReportedUntilTokenExpiry() {
            store.revoke("jti-M1", Instant.now().plusSeconds(600));

            assertThat(store.isRevoked("jti-M1")).isTrue();
        }

        @Test
        void entryPastTokenExpiryIsNoLongerRevoked() {
            store.revoke("jti-M2", Instant.now().minusSeconds(1));

            assertThat(store.isRevoked("jti-M2")).isFalse();
        }

        @Test
        void unknownJtiIsNotRevoked() {
            assertThat(store.isRevoked("jti-M3")).isFalse();
        }
    }
}
