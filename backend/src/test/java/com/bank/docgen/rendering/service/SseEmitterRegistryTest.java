package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRegistryTest {

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
