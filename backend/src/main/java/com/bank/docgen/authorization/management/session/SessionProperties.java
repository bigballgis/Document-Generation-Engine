package com.bank.docgen.authorization.management.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Management session policy (LR-B6).
 *
 * @param absoluteTtl     ISO-8601 duration for the absolute session limit measured from first
 *                        login ({@code sessionStartedAt}); renewals never extend past it.
 * @param revocationStore revocation list backend: {@code redis} (default) or {@code memory}
 *                        (transitional-test-only; forbidden in prod).
 */
@ConfigurationProperties(prefix = "docgen.session")
public record SessionProperties(String absoluteTtl, String revocationStore) {
}
