package com.bank.docgen.runtime.api;

import java.util.List;

public record BatchResultItemView(
        String itemId,
        String status,
        OutputOptionsView output,
        EncryptionSummaryView encryptionSummary,
        String documentId,
        List<String> fidelityWarnings
) {
}
