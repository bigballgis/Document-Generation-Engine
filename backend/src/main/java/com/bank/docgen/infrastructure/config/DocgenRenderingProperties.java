package com.bank.docgen.infrastructure.config;

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

    public boolean isPdfPageNumberStampingEnabled() {
        return pdfPageNumberStampingEnabled;
    }

    public void setPdfPageNumberStampingEnabled(boolean pdfPageNumberStampingEnabled) {
        this.pdfPageNumberStampingEnabled = pdfPageNumberStampingEnabled;
    }
}
