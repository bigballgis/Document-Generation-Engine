package com.bank.docgen.template.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * ADR-0042: set or clear the author-declared Microsoft Word page count for the in-flight DEV version.
 * Null clears the declaration (pagination delta enforcement skips).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateAuthorWordPageCountRequest(
        @Min(1)
        @Max(10_000)
        Integer authorWordPageCount
) {
}
