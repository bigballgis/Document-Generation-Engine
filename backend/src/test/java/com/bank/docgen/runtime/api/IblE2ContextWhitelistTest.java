package com.bank.docgen.runtime.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E2-001 / 002 — context whitelist accepts jurisdiction/product; unknown fields fail.
 */
class IblE2ContextWhitelistTest {

    private final ObjectMapper strictMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void deserializesJurisdictionAndProduct_bddE2001() throws Exception {
        ContextView context = strictMapper.readValue(
                """
                {"sourceSystem":"LOS","channel":"API","jurisdiction":"Hong Kong","product":"TRADE-LC","locale":"en-US"}
                """,
                ContextView.class
        );

        assertThat(context.jurisdiction()).isEqualTo("Hong Kong");
        assertThat(context.product()).isEqualTo("TRADE-LC");
        assertThat(context.channel()).isEqualTo("API");
    }

    @Test
    void unknownContextFieldRejected_bddE2002() {
        assertThatThrownBy(() -> strictMapper.readValue(
                """
                {"unknownDim":"x","channel":"API"}
                """,
                ContextView.class
        )).isInstanceOf(UnrecognizedPropertyException.class);
    }
}
