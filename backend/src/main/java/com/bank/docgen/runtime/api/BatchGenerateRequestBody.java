package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

public record BatchGenerateRequestBody(
        @NotNull OutputOptionsView output,
        @NotEmpty List<@Valid BatchGenerateItemBody> items,
        EncryptionOptionsView encryption,
        @NotBlank String requestId,
        @NotBlank String idempotencyKey,
        @Pattern(regexp = "^BATCH-[A-Za-z0-9]+$") String originalBatchId,
        ContextView context
) {
    public BatchGenerateRequestBody {
        items = DefensiveCopies.copyList(items);
    }

    public record BatchGenerateItemBody(
            @NotBlank String itemId,
            @NotNull Map<String, Object> variables,
            OutputOptionsView output,
            EncryptionOptionsView encryption
    ) {
    public BatchGenerateItemBody {
        variables = DefensiveCopies.copyMap(variables);
    }

    }
}
