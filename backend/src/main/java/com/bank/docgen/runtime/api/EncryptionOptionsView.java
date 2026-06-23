package com.bank.docgen.runtime.api;

import java.util.List;

public record EncryptionOptionsView(
        Boolean enabled,
        String openPassword,
        String ownerPassword,
        List<String> permissions
) {
}
