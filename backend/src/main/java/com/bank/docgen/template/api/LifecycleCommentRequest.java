package com.bank.docgen.template.api;

import jakarta.validation.constraints.NotBlank;

public record LifecycleCommentRequest(
        @NotBlank String commentSummary
) {
}
