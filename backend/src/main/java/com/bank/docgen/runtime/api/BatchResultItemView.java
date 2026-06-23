package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.ErrorDetail;
import java.util.List;

public record BatchResultItemView(
        String itemId,
        String status,
        OutputOptionsView output,
        EncryptionSummaryView encryptionSummary,
        String documentId,
        List<String> fidelityWarnings,
        ErrorDetail error
) {
    public BatchResultItemView(
            String itemId,
            String status,
            OutputOptionsView output,
            EncryptionSummaryView encryptionSummary,
            String documentId,
            List<String> fidelityWarnings
    ) {
        this(itemId, status, output, encryptionSummary, documentId, fidelityWarnings, null);
    }
}
