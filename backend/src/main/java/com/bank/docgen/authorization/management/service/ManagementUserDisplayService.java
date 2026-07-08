package com.bank.docgen.authorization.management.service;

import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ManagementUserDisplayService {

    private final ManagementUserRepository managementUserRepository;

    public ManagementUserDisplayService(ManagementUserRepository managementUserRepository) {
        this.managementUserRepository = managementUserRepository;
    }

    public Map<String, String> lookupDisplayNames(Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Map.of();
        }
        Set<String> normalized = usernames.stream()
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        if (normalized.isEmpty()) {
            return Map.of();
        }
        Map<String, String> found = new HashMap<>();
        for (ManagementUserEntity user : managementUserRepository.findByUsernameInAndDeletedAtIsNull(normalized)) {
            found.put(user.getUsername(), formatDisplayName(user.getDisplayName(), user.getUsername()));
        }
        Map<String, String> result = new HashMap<>();
        for (String username : normalized) {
            result.put(username, found.getOrDefault(username, username));
        }
        return result;
    }

    static String formatDisplayName(String displayName, String username) {
        if (displayName == null || displayName.isBlank()) {
            return username;
        }
        if (username == null || username.isBlank()) {
            return displayName;
        }
        return displayName + " (" + username + ")";
    }
}
