package com.bank.docgen.runtime.api;

import java.util.List;

public record EncryptionCapabilitiesView(
        boolean docxEnabled,
        boolean pdfEnabled,
        List<String> permissions
) {
}
