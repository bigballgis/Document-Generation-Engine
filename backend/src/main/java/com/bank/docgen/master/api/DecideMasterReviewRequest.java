package com.bank.docgen.master.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DecideMasterReviewRequest(
        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
        @Size(max = 2048) String commentSummary,
        Boolean exceptionIntervention,
        @Size(max = 2048) String exceptionReason,
        Boolean secondaryConfirmed
) {
    public DecideMasterReviewRequest(String decision, String commentSummary) {
        this(decision, commentSummary, null, null, null);
    }
}
