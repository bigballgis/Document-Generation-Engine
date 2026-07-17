package com.bank.docgen.apimgmt.service;

import java.util.List;

/**
 * SPI for resolving AD Group membership for a runtime access account
 * (BDD-PRR-B02-LDAP-SPI / ADR-0010 / ADR-0054).
 *
 * <p>Consumers (e.g. {@code ApiCredentialAuthenticationFilter}) must depend on this interface only.
 * LAB/local/dev/test may bind {@link ConfigAdGroupResolver}; claimed production without a real
 * directory adapter remains fail-closed via {@code AdGroupResolverGuard}.
 *
 * <p>Blank/null/unknown {@code accessAccount} → empty list (fail-closed authorization).
 * Group/policy intersection is <em>not</em> part of this SPI.
 */
public interface AdGroupResolver {

    List<String> resolveGroups(String accessAccount);
}
