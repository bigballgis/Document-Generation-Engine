package com.bank.docgen.infrastructure.config;

import com.bank.docgen.rendering.service.SseEmitterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PreviewSseConfig {

    @Bean(name = "previewSseRegistry")
    public SseEmitterRegistry previewSseRegistry() {
        return new SseEmitterRegistry();
    }

    @Bean(name = "batchSseRegistry")
    public SseEmitterRegistry batchSseRegistry() {
        return new SseEmitterRegistry();
    }
}
