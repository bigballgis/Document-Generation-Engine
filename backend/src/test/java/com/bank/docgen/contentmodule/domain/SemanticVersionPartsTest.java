package com.bank.docgen.contentmodule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SemanticVersionPartsTest {

    @Test
    void parse_ordersOneTenAboveOneNine_fosW6_4() {
        SemanticVersionParts older = SemanticVersionParts.parse("1.9");
        SemanticVersionParts newer = SemanticVersionParts.parse("1.10");
        assertThat(newer.major()).isEqualTo(older.major());
        assertThat(newer.minor()).isGreaterThan(older.minor());
    }

    @Test
    void parse_handlesThreePartAndSuffix() {
        SemanticVersionParts parts = SemanticVersionParts.parse("2.3.4-beta");
        assertThat(parts).isEqualTo(new SemanticVersionParts(2, 3, 4));
    }
}
