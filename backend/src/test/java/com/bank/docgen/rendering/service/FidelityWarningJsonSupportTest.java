package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FidelityWarningJsonSupportTest {

    private FidelityWarningJsonSupport support;

    @BeforeEach
    void setUp() {
        support = new FidelityWarningJsonSupport(new ObjectMapper());
    }

    @Test
    void roundTrip_preservesViewedFlag() {
        List<FidelityWarningView> warnings = List.of(
                new FidelityWarningView("CODE_A", "key.a", "loc", "ANCHOR", Boolean.FALSE),
                new FidelityWarningView("CODE_B", "key.b", null, null, Boolean.TRUE)
        );

        String json = support.writeWarnings(warnings);
        List<FidelityWarningView> restored = support.readWarnings(json);

        assertThat(restored).hasSize(2);
        assertThat(restored.get(0).viewed()).isFalse();
        assertThat(restored.get(1).viewed()).isTrue();
    }

    @Test
    void countUnviewed_excludesViewedWarnings() {
        List<FidelityWarningView> warnings = List.of(
                new FidelityWarningView("A", "k", null, null, Boolean.FALSE),
                new FidelityWarningView("B", "k", null, null, Boolean.TRUE),
                new FidelityWarningView("C", "k", null, null, null)
        );

        assertThat(support.countUnviewed(warnings)).isEqualTo(2);
    }
}
