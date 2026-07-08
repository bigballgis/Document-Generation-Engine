package com.bank.docgen.apimgmt.api;

import com.bank.docgen.apimgmt.domain.ApiAccessAlertSeverity;
import com.bank.docgen.apimgmt.domain.ApiAccessAlertType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiAccessAlertView(
        ApiAccessAlertType alertType,
        ApiAccessAlertSeverity severity,
        UUID templateId,
        String templateExternalId,
        String templateName,
        String groupCode,
        String detailMessageKey,
        String hubDeepLinkPath,
        String credentialExternalId,
        Instant expiresAt
) {
}
