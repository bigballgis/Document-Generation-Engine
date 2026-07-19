package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * IBL-D2 / F21 — CI lane gate for {@code -Plibreoffice-ci}.
 *
 * <p>Default verify: {@link LibreOfficeTestSupport#requireSoffice(String)} skips when
 * {@code soffice} is absent. Under the mandatory profile the same check fails the build.
 */
@Tag(LibreOfficeTestSupport.TAG)
class LibreOfficeMandatoryLaneGateTest {

    @Test
    void sofficeIsAvailableOnMandatoryLibreOfficeLane() {
        LibreOfficeTestSupport.requireSoffice("IBL-D2 / F21 LibreOffice CI lane gate");
        assertThat(LibreOfficeTestSupport.isSofficeAvailable())
                .as("soffice must be discoverable when the mandatory LO lane is green")
                .isTrue();
    }
}
