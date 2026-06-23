package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record BatchGenerateRequestBody(
        @NotNull OutputOptionsView output,
        @NotEmpty List<@Valid BatchGenerateItemBody> items,
        EncryptionOptionsView encryption,
        @NotBlank String requestId,
        @NotBlank String idempotencyKey
) {
    public record BatchGenerateItemBody(
            @NotBlank String itemId,
            @NotNull Map<String, Object> variables,
            OutputOptionsView output,
            EncryptionOptionsView encryption
    ) {
    }
}
