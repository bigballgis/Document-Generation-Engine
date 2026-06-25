package com.bank.docgen.rendering.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchTestGenerateRequest(
        @NotEmpty @Size(max = 50) List<@NotBlank String> testDataSetIds
) {
}
