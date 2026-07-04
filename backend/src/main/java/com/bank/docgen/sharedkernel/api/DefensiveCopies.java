package com.bank.docgen.sharedkernel.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared defensive copies for immutable API DTOs (SpotBugs EI_EXPOSE_REP2 ratchet).
 */
public final class DefensiveCopies {

    private DefensiveCopies() {
    }

    public static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public static List<String> copyStringList(List<String> values) {
        return copyList(values);
    }

    public static <T> Set<T> copySet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    public static <K, V> Map<K, V> copyMap(Map<K, V> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
