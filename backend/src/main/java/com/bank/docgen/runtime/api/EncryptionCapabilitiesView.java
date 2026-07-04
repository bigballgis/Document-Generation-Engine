package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record EncryptionCapabilitiesView(
        boolean docxEnabled,
        boolean pdfEnabled,
        List<String> permissions
) {
    public EncryptionCapabilitiesView {
        permissions = DefensiveCopies.copyStringList(permissions);
    }
}
