package com.bank.docgen.infrastructure.config;

import com.bank.docgen.rendering.service.SseEmitterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PreviewSseConfig {

    @Bean(name = "previewSseRegistry")
    public SseEmitterRegistry previewSseRegistry(DocgenRenderingProperties renderingProperties) {
        return new SseEmitterRegistry(
                renderingProperties.getSseTimeout(),
                renderingProperties.getSseHeartbeatInterval()
        );
    }

    @Bean(name = "batchSseRegistry")
    public SseEmitterRegistry batchSseRegistry(DocgenRenderingProperties renderingProperties) {
        return new SseEmitterRegistry(
                renderingProperties.getSseTimeout(),
                renderingProperties.getSseHeartbeatInterval()
        );
    }
}
