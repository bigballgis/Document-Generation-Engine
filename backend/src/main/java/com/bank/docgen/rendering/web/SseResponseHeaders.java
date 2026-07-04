package com.bank.docgen.rendering.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * LR-B3: shared anti-buffering headers for progress-stream (SSE) responses.
 * {@code X-Accel-Buffering: no} disables proxy response buffering (nginx);
 * {@code Cache-Control: no-cache} keeps intermediaries from caching the stream.
 */
final class SseResponseHeaders {

    static final String X_ACCEL_BUFFERING = "X-Accel-Buffering";

    private SseResponseHeaders() {
    }

    static ResponseEntity<SseEmitter> withAntiBufferingHeaders(SseEmitter emitter) {
        return ResponseEntity.ok()
                .header(X_ACCEL_BUFFERING, "no")
                .header("Cache-Control", "no-cache")
                .body(emitter);
    }
}
