package com.bank.docgen.apimgmt.service;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * LAB / local / dev / test AD Group resolver backed by YAML {@code account-groups}.
 *
 * <p>Not a production directory adapter (ADR-0054). Production consumers must inject
 * {@link AdGroupResolver}; do not hard-couple to this concrete type.
 */
@Service
public class ConfigAdGroupResolver implements AdGroupResolver {

    private final AdGroupResolverProperties properties;

    public ConfigAdGroupResolver(AdGroupResolverProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> resolveGroups(String accessAccount) {
        if (accessAccount == null || accessAccount.isBlank()) {
            return List.of();
        }
        return properties.getAccountGroups().getOrDefault(accessAccount, List.of());
    }

    /**
     * Transitional convenience for tests — production consumers must use
     * {@link AdGroupAuthorization#isAuthorized(AdGroupResolver, String, List)} with the SPI.
     */
    public boolean isAuthorized(String accessAccount, List<String> allowedAdGroups) {
        return AdGroupAuthorization.isAuthorized(this, accessAccount, allowedAdGroups);
    }
}
