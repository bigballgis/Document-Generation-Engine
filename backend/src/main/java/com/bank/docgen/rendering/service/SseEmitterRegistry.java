package com.bank.docgen.rendering.service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Registry that bridges async generation tasks with SSE emitters.
 * Buffers events when the client has not yet connected; drains them on registration.
 *
 * <p>LR-B3: sends an SSE comment heartbeat ({@code : keep-alive}) on a fixed cadence so
 * idle streams survive proxy read timeouts, and completes all emitters on shutdown so
 * clients see a clean stream end during graceful drain (LR-B5).</p>
 */
public class SseEmitterRegistry {

    /** LR-B3: covers the longest expected batch-test stream plus margin (config-driven). */
    public static final Duration DEFAULT_SSE_TIMEOUT = Duration.ofMinutes(15);

    /** LR-B3: keep-alive cadence; proxies must allow at least 3x this as read timeout. */
    public static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(20);

    private static final Logger LOG = LoggerFactory.getLogger(SseEmitterRegistry.class);
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    record PendingEvent(String eventName, Object data) {
    }

    private final long timeoutMillis;
    private final ScheduledExecutorService heartbeatExecutor;
    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Queue<PendingEvent>> pendingEvents = new ConcurrentHashMap<>();

    public SseEmitterRegistry() {
        this(DEFAULT_SSE_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
    }

    public SseEmitterRegistry(Duration sseTimeout, Duration heartbeatInterval) {
        this.timeoutMillis = sseTimeout.toMillis();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory());
        long heartbeatMillis = heartbeatInterval.toMillis();
        this.heartbeatExecutor.scheduleAtFixedRate(
                this::sendHeartbeats,
                heartbeatMillis,
                heartbeatMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Registers an SSE emitter for the given operation ID.
     * Any buffered events are immediately flushed to the emitter.
     */
    public SseEmitter register(UUID operationId) {
        SseEmitter emitter = newEmitter(timeoutMillis);
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

    /**
     * Stops the heartbeat sweep and completes all registered emitters (LR-B5 drain).
     */
    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
        for (UUID operationId : emitters.keySet()) {
            complete(operationId);
        }
    }

    /** Factory seam so tests can observe emitted frames. */
    protected SseEmitter newEmitter(long emitterTimeoutMillis) {
        return new SseEmitter(emitterTimeoutMillis);
    }

    boolean isRegistered(UUID operationId) {
        return emitters.containsKey(operationId);
    }

    /** Active connected emitters (excludes pending buffered-only operations). */
    public int activeCount() {
        return emitters.size();
    }

    boolean isHeartbeatStopped() {
        return heartbeatExecutor.isShutdown();
    }

    private void sendHeartbeats() {
        for (Map.Entry<UUID, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception ex) {
                LOG.debug("SSE heartbeat failed for operation {}: {}", entry.getKey(), ex.getMessage());
                cleanUp(entry.getKey());
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

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "sse-heartbeat-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
