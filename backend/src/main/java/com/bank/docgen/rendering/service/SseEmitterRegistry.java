package com.bank.docgen.rendering.service;

import java.io.IOException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Registry that bridges async generation tasks with SSE emitters.
 * Buffers events when the client has not yet connected; drains them on registration.
 */
public class SseEmitterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SseEmitterRegistry.class);
    private static final long DEFAULT_TIMEOUT_MS = 3 * 60 * 1000L;

    record PendingEvent(String eventName, Object data) {
    }

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Queue<PendingEvent>> pendingEvents = new ConcurrentHashMap<>();

    /**
     * Registers an SSE emitter for the given operation ID.
     * Any buffered events are immediately flushed to the emitter.
     */
    public SseEmitter register(UUID operationId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        emitters.put(operationId, emitter);
        emitter.onCompletion(() -> cleanUp(operationId));
        emitter.onTimeout(() -> cleanUp(operationId));
        emitter.onError(ex -> cleanUp(operationId));

        Queue<PendingEvent> buffered = pendingEvents.remove(operationId);
        if (buffered != null) {
            PendingEvent event = buffered.poll();
            while (event != null) {
                sendToEmitter(emitter, event.eventName(), event.data());
                event = buffered.poll();
            }
        }
        return emitter;
    }

    /**
     * Sends an event to the registered emitter, or buffers it if none is registered yet.
     */
    public void send(UUID operationId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(operationId);
        if (emitter != null) {
            sendToEmitter(emitter, eventName, data);
        } else {
            pendingEvents
                    .computeIfAbsent(operationId, k -> new ConcurrentLinkedQueue<>())
                    .add(new PendingEvent(eventName, data));
        }
    }

    /**
     * Completes the emitter (closes SSE connection) and removes it from the registry.
     */
    public void complete(UUID operationId) {
        SseEmitter emitter = emitters.remove(operationId);
        pendingEvents.remove(operationId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ex) {
                LOG.debug("SSE emitter already closed for operation {}", operationId);
            }
        }
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ex) {
            LOG.debug("Failed to send SSE event '{}' for emitter: {}", eventName, ex.getMessage());
        }
    }

    private void cleanUp(UUID operationId) {
        emitters.remove(operationId);
        pendingEvents.remove(operationId);
    }
}
