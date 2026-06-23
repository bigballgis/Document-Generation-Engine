package com.bank.docgen.apimgmt.service;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

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

    public boolean isAuthorized(String accessAccount, List<String> allowedAdGroups) {
        if (allowedAdGroups == null || allowedAdGroups.isEmpty()) {
            return false;
        }
        List<String> callerGroups = resolveGroups(accessAccount);
        if (callerGroups.isEmpty()) {
            return false;
        }
        return !Collections.disjoint(callerGroups, allowedAdGroups);
    }
}
