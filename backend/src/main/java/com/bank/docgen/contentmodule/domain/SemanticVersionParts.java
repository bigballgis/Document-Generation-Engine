package com.bank.docgen.contentmodule.domain;

/**
 * FOS-W6-4: parse {@code major.minor.patch} prefixes for numeric ordering.
 * Non-numeric suffixes (e.g. {@code 1.2.3-beta}) are ignored after the numeric core.
 */
public record SemanticVersionParts(int major, int minor, int patch) {

    public static SemanticVersionParts parse(String semanticVersion) {
        if (semanticVersion == null || semanticVersion.isBlank()) {
            return new SemanticVersionParts(0, 0, 0);
        }
        String core = semanticVersion.trim().replaceAll("[^0-9.].*$", "");
        String[] parts = core.split("\\.", -1);
        return new SemanticVersionParts(
                parsePart(parts, 0),
                parsePart(parts, 1),
                parsePart(parts, 2)
        );
    }

    private static int parsePart(String[] parts, int index) {
        if (index >= parts.length || parts[index] == null || parts[index].isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
