package com.bank.docgen.master.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitMasterReviewRequest(
        @NotBlank @Size(max = 2048) String changeSummary
) {
}
