package com.bank.docgen.authorization.management.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogPageSupportTest {

    @Test
    void normalizePage_clampsNegativeToZero() {
        assertThat(CatalogPageSupport.normalizePage(null)).isZero();
        assertThat(CatalogPageSupport.normalizePage(-1)).isZero();
        assertThat(CatalogPageSupport.normalizePage(2)).isEqualTo(2);
    }

    @Test
    void normalizeSize_defaultsAndClampsToMax() {
        assertThat(CatalogPageSupport.normalizeSize(null)).isEqualTo(20);
        assertThat(CatalogPageSupport.normalizeSize(0)).isEqualTo(20);
        assertThat(CatalogPageSupport.normalizeSize(-5)).isEqualTo(20);
        assertThat(CatalogPageSupport.normalizeSize(50)).isEqualTo(50);
        assertThat(CatalogPageSupport.normalizeSize(101)).isEqualTo(100);
        assertThat(CatalogPageSupport.normalizeSize(100)).isEqualTo(100);
    }

    @Test
    void parseSort_fallsBackToGroupCodeAscForUnknown() {
        assertThat(CatalogSortKey.parse(null)).isEqualTo(CatalogSortKey.GROUP_CODE_ASC);
        assertThat(CatalogSortKey.parse("bogus")).isEqualTo(CatalogSortKey.GROUP_CODE_ASC);
        assertThat(CatalogSortKey.parse("groupAsc")).isEqualTo(CatalogSortKey.GROUP_CODE_ASC);
        assertThat(CatalogSortKey.parse("updatedAtDesc")).isEqualTo(CatalogSortKey.UPDATED_AT_DESC);
        assertThat(CatalogSortKey.parse("externalIdAsc", CatalogSortKey.EXTERNAL_ID_ASC))
                .isEqualTo(CatalogSortKey.EXTERNAL_ID_ASC);
        assertThat(CatalogSortKey.parse("externalIdAsc")).isEqualTo(CatalogSortKey.GROUP_CODE_ASC);
        assertThat(CatalogSortKey.parse("moduleCodeAsc", CatalogSortKey.MODULE_CODE_ASC))
                .isEqualTo(CatalogSortKey.MODULE_CODE_ASC);
    }
}
