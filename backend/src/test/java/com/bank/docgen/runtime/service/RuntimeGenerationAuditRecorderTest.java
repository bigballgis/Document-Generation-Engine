package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuntimeGenerationAuditRecorderTest {

    @Test
    void hashIdempotencyKeyReturnsStableSha256Hex() {
        String first = RuntimeGenerationAuditRecorder.hashIdempotencyKey("idem-key-1");
        String second = RuntimeGenerationAuditRecorder.hashIdempotencyKey("idem-key-1");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void hashIdempotencyKeyReturnsNullForBlankInput() {
        assertThat(RuntimeGenerationAuditRecorder.hashIdempotencyKey(null)).isNull();
        assertThat(RuntimeGenerationAuditRecorder.hashIdempotencyKey("  ")).isNull();
    }
}
