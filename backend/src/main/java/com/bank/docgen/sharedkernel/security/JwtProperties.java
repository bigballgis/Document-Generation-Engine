package com.bank.docgen.sharedkernel.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.jwt")
public record JwtProperties(String secret, String accessTokenTtl) {
}
