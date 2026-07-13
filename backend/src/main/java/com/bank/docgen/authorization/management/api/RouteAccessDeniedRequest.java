package com.bank.docgen.authorization.management.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RouteAccessDeniedRequest(
        @NotBlank
        @Size(max = 256)
        String routeKey,
        @NotBlank
        @Size(max = 128)
        String traceId
) {
}
