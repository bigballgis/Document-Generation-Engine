package com.bank.docgen.sharedkernel.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void createAndParseAccessToken() {
        JwtProperties properties = new JwtProperties(
                "test-jwt-secret-at-least-32-bytes-long!!",
                "PT15M");
        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        String token = jwtTokenService.createAccessToken("12345678");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.parseSubject(token)).isEqualTo("12345678");
    }
}
