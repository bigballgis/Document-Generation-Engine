package com.bank.docgen.master.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CE-U06 — update only {@code displayLabel} on a current writable revision-line anchor.
 * {@code anchorId} and {@code documentSequence} are not mutable via this request.
 */
public record UpdateMasterAnchorDisplayLabelRequest(
        @NotBlank @Size(max = 256) String displayLabel
) {
}
