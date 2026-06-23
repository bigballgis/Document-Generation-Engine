package com.bank.docgen.sharedkernel.api;

import java.util.List;

public record EncryptionOptionsView(
        Boolean enabled,
        String openPassword,
        String ownerPassword,
        List<String> permissions
) {
}
