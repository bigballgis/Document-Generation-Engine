package com.bank.docgen.template.port;

/**
 * Normalized composition axes (trim; blank → absent) for ADR-0063 inclusion evaluation.
 */
public record CompositionInclusionAxes(
        String jurisdiction,
        String product,
        String channel
) {
    public static CompositionInclusionAxes of(String jurisdiction, String product, String channel) {
        return new CompositionInclusionAxes(normalize(jurisdiction), normalize(product), normalize(channel));
    }

    public static CompositionInclusionAxes empty() {
        return new CompositionInclusionAxes(null, null, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
