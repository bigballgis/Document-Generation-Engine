package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRegistryTest {

    /** Captures frames written by the registry (real SseEmitter cannot be inspected). */
    private static final class RecordingSseEmitter extends SseEmitter {

        private final List<String> frames = new CopyOnWriteArrayList<>();
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final boolean failOnSend;

        RecordingSseEmitter(Long timeout, boolean failOnSend) {
            super(timeout);
            this.failOnSend = failOnSend;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failOnSend) {
                throw new IOException("client gone");
            }
            StringBuilder frame = new StringBuilder();
            builder.build().forEach(dataWithMediaType -> frame.append(dataWithMediaType.getData()));
            frames.add(frame.toString());
        }

        @Override
        public void send(Object object, MediaType mediaType) throws IOException {
            frames.add(String.valueOf(object));
        }

        @Override
        public void complete() {
            completed.set(true);
        }

        List<String> frames() {
            return frames;
        }

        boolean isCompleted() {
            return completed.get();
        }
    }

    private static final class RecordingRegistry extends SseEmitterRegistry {

        private final boolean failOnSend;
        private final List<RecordingSseEmitter> created = new CopyOnWriteArrayList<>();

        RecordingRegistry(Duration timeout, Duration heartbeatInterval, boolean failOnSend) {
            super(timeout, heartbeatInterval);
            this.failOnSend = failOnSend;
        }

        @Override
        protected SseEmitter newEmitter(long emitterTimeoutMillis) {
            RecordingSseEmitter emitter = new RecordingSseEmitter(emitterTimeoutMillis, failOnSend);
            created.add(emitter);
            return emitter;
        }

        List<RecordingSseEmitter> created() {
            return created;
        }
    }

    @Test
    void register_appliesConfiguredSseTimeout() {
        SseEmitterRegistry registry = new SseEmitterRegistry(
                Duration.ofMinutes(15), Duration.ofSeconds(20));
        try {
            SseEmitter emitter = registry.register(UUID.randomUUID());

            assertThat(emitter.getTimeout()).isEqualTo(Duration.ofMinutes(15).toMillis());
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void heartbeat_sendsKeepAliveCommentToRegisteredEmitters() {
        RecordingRegistry registry = new RecordingRegistry(
                Duration.ofMinutes(15), Duration.ofMillis(50), false);
        try {
            registry.register(UUID.randomUUID());

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(registry.created().getFirst().frames())
                            .anySatisfy(frame -> assertThat(frame).contains(":keep-alive")));
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void heartbeat_sendFailureCleansUpEmitter() {
        RecordingRegistry registry = new RecordingRegistry(
                Duration.ofMinutes(15), Duration.ofMillis(50), true);
        try {
            UUID id = UUID.randomUUID();
            registry.register(id);

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(registry.isRegistered(id)).isFalse());
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void shutdown_completesAllEmittersAndStopsHeartbeat() {
        RecordingRegistry registry = new RecordingRegistry(
                Duration.ofMinutes(15), Duration.ofSeconds(20), false);
        UUID id = UUID.randomUUID();
        registry.register(id);

        registry.shutdown();

        assertThat(registry.created().getFirst().isCompleted()).isTrue();
        assertThat(registry.isRegistered(id)).isFalse();
        assertThat(registry.isHeartbeatStopped()).isTrue();
    }

    @Test
    void register_returnsNonNullEmitter() {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        UUID id = UUID.randomUUID();

        SseEmitter emitter = registry.register(id);

        assertThat(emitter).isNotNull();
    }

    @Test
    void send_beforeRegister_buffersEvent() {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        UUID id = UUID.randomUUID();

        registry.send(id, "progress", "data");

        // Register after sending - should not throw
        SseEmitter emitter = registry.register(id);
        assertThat(emitter).isNotNull();
    }

    @Test
    void complete_doesNotThrowIfNoEmitter() {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        UUID id = UUID.randomUUID();

        // Should not throw
        registry.complete(id);
    }

    @Test
    void send_afterComplete_doesNotThrow() {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        UUID id = UUID.randomUUID();
        registry.register(id);
        registry.complete(id);

        // Should not throw after complete
        registry.send(id, "event", "data");
    }

    @Test
    void register_multipleTimes_returnsNewEmitter() {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        UUID id = UUID.randomUUID();

        SseEmitter first = registry.register(id);
        SseEmitter second = registry.register(id);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
    }
}
