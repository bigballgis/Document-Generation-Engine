package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotNull;

public record PasteCleanRequest(
        @NotNull String sourceHtml,
        String prePasteStructuredContentJson
) {
}
