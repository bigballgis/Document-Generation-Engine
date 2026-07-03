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
}
