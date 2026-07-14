package com.bank.docgen.master.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParser;
import org.junit.jupiter.api.Test;

class TestMasterDocxFactoryTest {

    @Test
    void factoryDocxHasParsableStyles() throws Exception {
        byte[] docx = TestMasterDocxFactory.buildWithAnchorText("{{anchor:HEADER}} body");
        var catalog = MasterDocxStyleCatalogParser.parse(docx);
        assertThat(catalog.hasDocDefaults()).isTrue();
        assertThat(catalog.find("Normal")).isNotNull();
        assertThat(catalog.find("BodyText")).isNotNull();
        assertThat(catalog.find("ClauseBody")).isNotNull();
    }
}
