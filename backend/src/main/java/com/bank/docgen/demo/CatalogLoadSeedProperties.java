package com.bank.docgen.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LR-C5 catalog load seed (≥500 templates across groups) for pagination / p95 evidence.
 *
 * <p>Enable with {@code DOCGEN_SEED_CATALOG_LOAD=true} or
 * {@code docgen.catalog-load-seed.enabled=true}.</p>
 */
@ConfigurationProperties(prefix = "docgen.catalog-load-seed")
public class CatalogLoadSeedProperties {

    /**
     * When true, seeds ≥{@code templateCount} draft templates across multiple groups (idempotent).
     */
    private boolean enabled = false;

    /** Target template row count (default 500). */
    private int templateCount = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTemplateCount() {
        return templateCount;
    }

    public void setTemplateCount(int templateCount) {
        this.templateCount = templateCount;
    }
}
