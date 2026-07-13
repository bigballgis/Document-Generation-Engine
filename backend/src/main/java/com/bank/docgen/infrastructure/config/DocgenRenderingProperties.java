package com.bank.docgen.infrastructure.config;

import com.bank.docgen.rendering.service.SseEmitterRegistry;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.rendering")
public class DocgenRenderingProperties {

    private String libreOfficeCommand = "soffice";

    private String conversionMode = "cli";

    private String dockerContainerName = "docgen-libreoffice";

    /** CLI used to invoke Docker (override in tests with a fake docker script). */
    private String dockerCliCommand = "docker";

    private int conversionTimeoutSeconds = 120;

    private int conversionPoolSize = 2;

    /**
     * Bounded queue for PDF conversion tasks. Default {@code 0} isolates sync capacity to
     * {@link #conversionPoolSize} workers — saturated requests fail fast (SOR-P03) instead of
     * blocking servlet threads behind a deep queue.
     */
    private int conversionQueueCapacity = 0;

    /** Maximum generated artifact size (DOCX/PDF bytes) before persistence (SOR-P02). */
    private long maxGeneratedArtifactBytes = 52_428_800L;

    /**
     * LR-A7 / ADR-0042: pagination delta budget (pages). If |pdfPages - wordPages| exceeds
     * this, a fidelity warning fires; at 2x, a fidelity blocker. The budget is a pending
     * proposal until the user confirms — until then the delta is logged but not enforced.
     */
    private int paginationDeltaBudgetPages = 1;

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

    /**
     * F1-A3: when enabled, unresolved image/seal references may fall back to
     * {@code classpath:rendering/demo-images/}. Production profiles must keep this {@code false}.
     */
    private boolean demoClasspathImageTierEnabled = false;

    /**
     * LR-A6 / ADR-0043: post-assembly OOXML well-formedness gate. Defaults to {@code true}
     * (fail-closed). Disable only for emergency diagnostics — never in production acceptance.
     */
    private boolean ooxmlValidationEnabled = true;

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

    public String getDockerCliCommand() {
        return dockerCliCommand;
    }

    public void setDockerCliCommand(String dockerCliCommand) {
        this.dockerCliCommand = dockerCliCommand;
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

    public int getConversionQueueCapacity() {
        return conversionQueueCapacity;
    }

    public void setConversionQueueCapacity(int conversionQueueCapacity) {
        this.conversionQueueCapacity = conversionQueueCapacity;
    }

    public long getMaxGeneratedArtifactBytes() {
        return maxGeneratedArtifactBytes;
    }

    public void setMaxGeneratedArtifactBytes(long maxGeneratedArtifactBytes) {
        this.maxGeneratedArtifactBytes = maxGeneratedArtifactBytes;
    }

    public int getPaginationDeltaBudgetPages() {
        return paginationDeltaBudgetPages;
    }

    public void setPaginationDeltaBudgetPages(int paginationDeltaBudgetPages) {
        this.paginationDeltaBudgetPages = paginationDeltaBudgetPages;
    }

    public boolean isPdfPageNumberStampingEnabled() {
        return pdfPageNumberStampingEnabled;
    }

    public void setPdfPageNumberStampingEnabled(boolean pdfPageNumberStampingEnabled) {
        this.pdfPageNumberStampingEnabled = pdfPageNumberStampingEnabled;
    }

    public boolean isDemoClasspathImageTierEnabled() {
        return demoClasspathImageTierEnabled;
    }

    public void setDemoClasspathImageTierEnabled(boolean demoClasspathImageTierEnabled) {
        this.demoClasspathImageTierEnabled = demoClasspathImageTierEnabled;
    }

    public boolean isOoxmlValidationEnabled() {
        return ooxmlValidationEnabled;
    }

    public void setOoxmlValidationEnabled(boolean ooxmlValidationEnabled) {
        this.ooxmlValidationEnabled = ooxmlValidationEnabled;
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
