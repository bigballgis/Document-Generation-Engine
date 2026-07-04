package com.bank.docgen.runtime.security;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;
import java.util.UUID;

public record RuntimeSessionClaims(
        UUID credentialId,
        String credentialExternalId,
        UUID templateId,
        String templateExternalId,
        String accessAccount,
        List<String> callerAdGroups
) {
    public RuntimeSessionClaims {
        callerAdGroups = DefensiveCopies.copyStringList(callerAdGroups);
    }
}
