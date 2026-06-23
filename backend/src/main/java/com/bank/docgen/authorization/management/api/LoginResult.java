package com.bank.docgen.authorization.management.api;

public record LoginResult(
        String accessToken,
        String tokenType,
        ManagementSessionView session
) {
}
