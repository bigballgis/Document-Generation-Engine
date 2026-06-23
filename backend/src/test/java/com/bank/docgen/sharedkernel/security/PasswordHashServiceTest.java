package com.bank.docgen.sharedkernel.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHashServiceTest {

    private final PasswordHashService passwordHashService = new PasswordHashService();

    @Test
    void hashAndVerifyRoundTrip() {
        String raw = "TestPassword-123456";
        String encoded = passwordHashService.hash(raw);

        assertThat(encoded).isNotBlank();
        assertThat(encoded).isNotEqualTo(raw);
        assertThat(passwordHashService.matches(raw, encoded)).isTrue();
        assertThat(passwordHashService.matches("wrong-password", encoded)).isFalse();
    }
}
