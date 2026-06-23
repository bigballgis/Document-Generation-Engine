package com.bank.docgen.audit.service;

import org.springframework.stereotype.Service;

@Service
public class AuditMaskingService {

    public String maskActorSummary(String actorSummary) {
        if (actorSummary == null || actorSummary.isBlank()) {
            return "****";
        }
        if (actorSummary.length() <= 4) {
            return "****";
        }
        return actorSummary.substring(0, 2) + "****" + actorSummary.substring(actorSummary.length() - 2);
    }

    public String maskCredentialFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return null;
        }
        if (fingerprint.length() <= 4) {
            return "****";
        }
        return fingerprint.substring(0, 4) + "****";
    }
}
