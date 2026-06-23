package com.bank.docgen.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.rendering")
public class DocgenRenderingProperties {

    private String libreOfficeCommand = "soffice";

    private String conversionMode = "cli";

    private String dockerContainerName = "docgen-libreoffice";

    private int conversionTimeoutSeconds = 120;

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
}
