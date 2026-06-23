package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import java.util.Map;

public record GenerateRequestBody(
        OutputOptionsView output,
        Map<String, Object> variables,
        EncryptionOptionsView encryption,
        String requestId,
        String idempotencyKey
) {
}
