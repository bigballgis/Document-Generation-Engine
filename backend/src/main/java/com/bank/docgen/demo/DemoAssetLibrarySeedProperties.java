package com.bank.docgen.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wave 8 optional demo/验收 managed Asset Library seed (W8-C2).
 *
 * <p>Default {@code false}: production / true-prod keep honest empty catalogs.
 * Enable only on demo/验收 stacks via {@code DOCGEN_SEED_DEMO_ASSET_LIBRARY=true}.
 */
@ConfigurationProperties(prefix = "docgen.demo-asset-library")
public class DemoAssetLibrarySeedProperties {

    /**
     * When true, seeds minimal managed {@code library_asset} rows (IMG-1 / SEAL-1) on startup.
     */
    private boolean seedEnabled = false;

    public boolean isSeedEnabled() {
        return seedEnabled;
    }

    public void setSeedEnabled(boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }
}
