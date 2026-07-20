package com.bank.docgen.documentbrand.api;

import jakarta.validation.constraints.Size;

public record PutGroupDefaultLegalEntityRequest(
        @Size(max = 64) String defaultLegalEntityCode
) {
}
