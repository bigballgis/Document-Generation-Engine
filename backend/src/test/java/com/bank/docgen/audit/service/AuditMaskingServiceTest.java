package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditMaskingServiceTest {

    private final AuditMaskingService maskingService = new AuditMaskingService();

    @Test
    void masksActorSummaryWithoutPlaintext() {
        String masked = maskingService.maskActorSummary("Global Admin (10000001)");
        assertThat(masked).doesNotContain("10000001");
        assertThat(masked).contains("****");
    }

    @Test
    void masksCredentialFingerprintPrefixOnly() {
        String masked = maskingService.maskCredentialFingerprint("fp-CRED-ABCD1234");
        assertThat(masked).startsWith("fp-C");
        assertThat(masked).endsWith("****");
        assertThat(masked).doesNotContain("ABCD1234");
    }
}
