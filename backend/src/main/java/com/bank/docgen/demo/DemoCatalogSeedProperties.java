package com.bank.docgen.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.demo-catalog")
public class DemoCatalogSeedProperties {

    /**
     * When true, seeds an approved demo master and draft template on startup (idempotent).
     */
    private boolean seedEnabled = false;

    public boolean isSeedEnabled() {
        return seedEnabled;
    }

    public void setSeedEnabled(boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }
}
