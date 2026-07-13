package com.bank.docgen.runtime.loadsmoke;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Always-on gate proofs for LR-D6: harness stays dark unless {@code docgen.loadSmoke=true}.
 */
class LoadSmokeFlagGateTest {

    @Test
    void flagDefaultsToDisabledSoNormalVerifySkipsDockerHarness() {
        String previous = System.getProperty(LoadSmokeFlag.PROPERTY);
        try {
            System.clearProperty(LoadSmokeFlag.PROPERTY);
            assertThat(LoadSmokeFlag.isEnabled()).isFalse();
            assertThat(LoadSmokeFlag.PROPERTY).isEqualTo("docgen.loadSmoke");
        } finally {
            if (previous == null) {
                System.clearProperty(LoadSmokeFlag.PROPERTY);
            } else {
                System.setProperty(LoadSmokeFlag.PROPERTY, previous);
            }
        }
    }

    @Test
    void flagParsesTrueWhenSet() {
        String previous = System.getProperty(LoadSmokeFlag.PROPERTY);
        try {
            System.setProperty(LoadSmokeFlag.PROPERTY, "true");
            assertThat(LoadSmokeFlag.isEnabled()).isTrue();
        } finally {
            if (previous == null) {
                System.clearProperty(LoadSmokeFlag.PROPERTY);
            } else {
                System.setProperty(LoadSmokeFlag.PROPERTY, previous);
            }
        }
    }

    /**
     * Companion disabled test: JUnit disables this method unless the smoke flag is set,
     * proving the Docker IT annotation pattern never executes under plain {@code mvn verify}.
     */
    @Test
    @EnabledIfSystemProperty(named = LoadSmokeFlag.PROPERTY, matches = "true")
    void dockerHarnessEnabledIfAnnotationIsInactiveWithoutFlag() {
        assertThat(LoadSmokeFlag.isEnabled()).isTrue();
    }
}
