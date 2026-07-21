package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DemoAssetLibrarySeedPropertiesTest {

    @Test
    void seedEnabledDefaultsToFalseForHonestEmptyProductDefault() {
        // BDD-SYS-NORM-W8-001 / W8-004 — production default keeps catalog empty without seed.
        DemoAssetLibrarySeedProperties properties = new DemoAssetLibrarySeedProperties();

        assertThat(properties.isSeedEnabled()).isFalse();
    }
}
