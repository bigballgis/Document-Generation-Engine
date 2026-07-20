package com.bank.docgen.runtime.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E4-004 — context whitelist accepts legalEntityCode.
 */
class IblE4ContextWhitelistTest {

    private final ObjectMapper strictMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void deserializesLegalEntityCode_bddE4004() throws Exception {
        ContextView context = strictMapper.readValue(
                """
                {"sourceSystem":"LOS","channel":"API","legalEntityCode":"LE-HK-001","locale":"en-US"}
                """,
                ContextView.class
        );

        assertThat(context.legalEntityCode()).isEqualTo("LE-HK-001");
    }
}
