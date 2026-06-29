package com.bank.docgen.apimgmt.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TemplateAdGroupAuthorizationCache {

    static final Duration TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<UUID, CacheEntry> entries = new ConcurrentHashMap<>();

    public Optional<List<String>> getAllowedGroups(UUID templateId) {
        CacheEntry entry = entries.get(templateId);
        if (entry == null || entry.isExpired()) {
            entries.remove(templateId);
            return Optional.empty();
        }
        return Optional.of(entry.allowedGroups());
    }

    public void rememberAllowedGroups(UUID templateId, List<String> allowedGroups) {
        entries.put(templateId, new CacheEntry(List.copyOf(allowedGroups), Instant.now().plus(TTL)));
    }

    public void invalidate(UUID templateId) {
        entries.remove(templateId);
    }

    private record CacheEntry(List<String> allowedGroups, Instant expiresAt) {

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
