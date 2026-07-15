package com.bank.docgen.runtime.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiInvocationRecordEntityTest {

    @Test
    void entityStoresBatchItemLinkage() {
        Instant now = Instant.now();
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                "INV-TEST01",
                InvocationKind.BATCH_ITEM,
                InvocationStatus.SUCCEEDED,
                "dev",
                UUID.randomUUID(),
                "TPL-001",
                UUID.randomUUID(),
                "svc-account",
                "req-1",
                "idem-1",
                "DEFAULT_ROUTE",
                null,
                "1.0.0",
                "DOCX",
                "SYNC_STREAM",
                "SUCCESS",
                null,
                "{}",
                "DOC-1",
                null,
                true,
                now.plusSeconds(3600),
                now.plusSeconds(1800),
                "BATCH-001",
                "INV-ROOT01",
                "item-1",
                null,
                null,
                "audit-1",
                true,
                now,
                now
        );

        assertThat(entity.getInvocationExternalId()).isEqualTo("INV-TEST01");
        assertThat(entity.getParentInvocationExternalId()).isEqualTo("INV-ROOT01");
        assertThat(entity.isBatch()).isTrue();
    }

    @Test
    void applyErrorEnvelope_persistsUnifiedFields() {
        Instant now = Instant.now();
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                "INV-FAIL01",
                InvocationKind.SINGLE,
                InvocationStatus.FAILED,
                "dev",
                UUID.randomUUID(),
                "TPL-001",
                UUID.randomUUID(),
                "svc-account",
                "req-1",
                "idem-1",
                "DEFAULT_ROUTE",
                null,
                "1.2.0",
                "DOCX",
                "SYNC_STREAM",
                "FAILURE",
                null,
                "{}",
                null,
                null,
                false,
                now.plusSeconds(3600),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                now,
                now
        );

        entity.applyErrorEnvelope(
                "REQUEST_BODY_INVALID",
                "RUNTIME",
                "api.error.validation.requestBodyInvalid",
                false,
                "Invalid body."
        );

        assertThat(entity.getErrorCode()).isEqualTo("REQUEST_BODY_INVALID");
        assertThat(entity.getErrorCategory()).isEqualTo("RUNTIME");
        assertThat(entity.getErrorMessageKey()).isEqualTo("api.error.validation.requestBodyInvalid");
        assertThat(entity.getErrorRetryable()).isFalse();
        assertThat(entity.getErrorMessage()).isEqualTo("Invalid body.");
    }
}
