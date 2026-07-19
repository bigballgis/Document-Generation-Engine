package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

/**
 * IBL-D2 / F21 — prove optional skip vs mandatory fail-closed without requiring a real soffice.
 */
class LibreOfficeTestSupportTest {

    @AfterEach
    void clearMandatoryProperty() {
        System.clearProperty(LibreOfficeTestSupport.MANDATORY_PROPERTY);
    }

    @Test
    void missingCommandIsNeverAvailable() {
        assertThat(LibreOfficeTestSupport.isSofficeAvailable(LibreOfficeTestSupport.MISSING_COMMAND_FOR_TESTS))
                .isFalse();
    }

    @Test
    void optionalMode_abortsWhenSofficeAbsent() {
        System.setProperty(LibreOfficeTestSupport.MANDATORY_PROPERTY, "false");

        assertThatThrownBy(() -> LibreOfficeTestSupport.requireSofficeForCommand(
                        LibreOfficeTestSupport.MISSING_COMMAND_FOR_TESTS,
                        "unit proof optional skip"
                ))
                .isInstanceOf(TestAbortedException.class)
                .hasMessageContaining("optional local skip");
    }

    @Test
    void mandatoryMode_failsWhenSofficeAbsent() {
        System.setProperty(LibreOfficeTestSupport.MANDATORY_PROPERTY, "true");

        assertThatThrownBy(() -> LibreOfficeTestSupport.requireSofficeForCommand(
                        LibreOfficeTestSupport.MISSING_COMMAND_FOR_TESTS,
                        "unit proof mandatory fail-closed"
                ))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("mandatory lane")
                .hasMessageContaining("unit proof mandatory fail-closed");
    }

    @Test
    void propertyNameAndTagAreStableForCiDocs() {
        assertThat(LibreOfficeTestSupport.MANDATORY_PROPERTY).isEqualTo("docgen.libreoffice.mandatory");
        assertThat(LibreOfficeTestSupport.TAG).isEqualTo("libreoffice");
    }
}
