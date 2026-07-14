package com.bank.docgen.rendering.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MarkFidelityWarningViewedRequest(
        @NotNull @Min(0) Integer warningIndex
) {
}
