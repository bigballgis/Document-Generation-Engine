package com.bank.docgen.infrastructure.config;

import com.bank.docgen.rendering.service.SseEmitterRegistry;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.rendering")
public class DocgenRenderingProperties {

    private String libreOfficeCommand = "soffice";

    private String conversionMode = "cli";

    private String dockerContainerName = "docgen-libreoffice";

    private int conversionTimeoutSeconds = 120;

    private int conversionPoolSize = 2;

    /**
     * LR-B3: SSE emitter timeout for progress streams. Sized to the longest expected
     * batch-test run plus margin (previously hardcoded to 3 minutes in the registry).
     */
    private Duration sseTimeout = SseEmitterRegistry.DEFAULT_SSE_TIMEOUT;

    /** LR-B3: cadence of the {@code : keep-alive} SSE comment heartbeat. */
    private Duration sseHeartbeatInterval = SseEmitterRegistry.DEFAULT_HEARTBEAT_INTERVAL;

    /**
     * When enabled, strips Word {@code PAGE} fields before LibreOffice conversion and stamps
     * {@code Page X of Y} onto the PDF. Deferred by default until final PDF fidelity work.
     */
    private boolean pdfPageNumberStampingEnabled = false;

    public String getLibreOfficeCommand() {
        return libreOfficeCommand;
    }

    public void setLibreOfficeCommand(String libreOfficeCommand) {
        this.libreOfficeCommand = libreOfficeCommand;
    }

    public String getConversionMode() {
        return conversionMode;
    }

    public void setConversionMode(String conversionMode) {
        this.conversionMode = conversionMode;
    }

    public String getDockerContainerName() {
        return dockerContainerName;
    }

    public void setDockerContainerName(String dockerContainerName) {
        this.dockerContainerName = dockerContainerName;
    }

    public int getConversionTimeoutSeconds() {
        return conversionTimeoutSeconds;
    }

    public void setConversionTimeoutSeconds(int conversionTimeoutSeconds) {
        this.conversionTimeoutSeconds = conversionTimeoutSeconds;
    }

    public int getConversionPoolSize() {
        return conversionPoolSize;
    }

    public void setConversionPoolSize(int conversionPoolSize) {
        this.conversionPoolSize = conversionPoolSize;
    }

    public boolean isPdfPageNumberStampingEnabled() {
        return pdfPageNumberStampingEnabled;
    }

    public void setPdfPageNumberStampingEnabled(boolean pdfPageNumberStampingEnabled) {
        this.pdfPageNumberStampingEnabled = pdfPageNumberStampingEnabled;
    }

    public Duration getSseTimeout() {
        return sseTimeout;
    }

    public void setSseTimeout(Duration sseTimeout) {
        this.sseTimeout = sseTimeout;
    }

    public Duration getSseHeartbeatInterval() {
        return sseHeartbeatInterval;
    }

    public void setSseHeartbeatInterval(Duration sseHeartbeatInterval) {
        this.sseHeartbeatInterval = sseHeartbeatInterval;
    }
}
