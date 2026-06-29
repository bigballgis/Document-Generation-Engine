package com.bank.docgen.rendering.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FidelityWarningView(
        String code,
        String messageKey,
        String location,
        String artifact,
        Boolean viewed
) {

    public FidelityWarningView(String code, String messageKey) {
        this(code, messageKey, null, null, Boolean.FALSE);
    }
}
