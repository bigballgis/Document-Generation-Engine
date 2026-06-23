package com.bank.docgen.runtime.api;

import java.util.List;

public record EncryptionSummaryView(
        boolean enabled,
        String outputFormat,
        boolean openPasswordProvided,
        boolean ownerPasswordProvided,
        List<String> permissions
) {
    public static EncryptionSummaryView disabled(String outputFormat) {
        return new EncryptionSummaryView(false, outputFormat, false, false, List.of());
    }

    public static EncryptionSummaryView fromRequest(String outputFormat, EncryptionOptionsView encryption) {
        if (encryption == null || !Boolean.TRUE.equals(encryption.enabled())) {
            return disabled(outputFormat);
        }
        return new EncryptionSummaryView(
                true,
                outputFormat,
                encryption.openPassword() != null && !encryption.openPassword().isBlank(),
                encryption.ownerPassword() != null && !encryption.ownerPassword().isBlank(),
                encryption.permissions() == null ? List.of() : encryption.permissions()
        );
    }
}
