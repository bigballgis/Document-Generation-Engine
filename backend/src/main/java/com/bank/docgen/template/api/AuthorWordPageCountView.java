package com.bank.docgen.template.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthorWordPageCountView(
        String templateId,
        String devVersionId,
        Integer authorWordPageCount
) {
}
