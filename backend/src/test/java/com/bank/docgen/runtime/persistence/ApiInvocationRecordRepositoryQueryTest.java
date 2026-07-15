package com.bank.docgen.runtime.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.infrastructure.config.QuerydslConfig;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({ApiInvocationRecordRepositoryImpl.class, QuerydslConfig.class})
@ActiveProfiles("test")
class ApiInvocationRecordRepositoryQueryTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CREDENTIAL_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CREDENTIAL_B = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private ApiInvocationRecordRepository repository;

    @BeforeEach
    void seedRecords() {
        repository.deleteAll();
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        repository.saveAll(List.of(
                record(
                        "INV-001",
                        InvocationKind.SINGLE,
                        "SUCCESS",
                        CREDENTIAL_A,
                        "alpha-request-001",
                        now.minusSeconds(300),
                        now.plusSeconds(3600)
                ),
                record(
                        "INV-002",
                        InvocationKind.SINGLE,
                        "FAILED",
                        CREDENTIAL_A,
                        "beta-request-002",
                        now.minusSeconds(200),
                        now.plusSeconds(3600)
                ),
                record(
                        "INV-003",
                        InvocationKind.BATCH_ROOT,
                        "SUCCESS",
                        CREDENTIAL_B,
                        "gamma-request-003",
                        now.minusSeconds(100),
                        now.plusSeconds(3600)
                ),
                record(
                        "INV-004",
                        InvocationKind.BATCH_ITEM,
                        "SUCCESS",
                        CREDENTIAL_A,
                        "delta-request-004",
                        now.minusSeconds(50),
                        now.plusSeconds(3600)
                )
        ));
    }

    @Test
    void searchManagementInvocations_filtersByOutcomeKindCredentialAndRequestId() {
        Instant retentionAfter = Instant.parse("2026-06-15T11:00:00Z");
        Set<InvocationKind> kinds = EnumSet.of(
                InvocationKind.SINGLE,
                InvocationKind.BATCH_ROOT,
                InvocationKind.ASYNC_TASK
        );

        AuditSearchPage<ApiInvocationRecordEntity> page = repository.searchManagementInvocations(
                TEMPLATE_ID,
                kinds,
                retentionAfter,
                "FAILED",
                InvocationKind.SINGLE,
                "beta",
                null,
                null,
                CREDENTIAL_A,
                null,
                0,
                20
        );

        assertThat(page.content()).extracting(ApiInvocationRecordEntity::getInvocationExternalId)
                .containsExactly("INV-002");
    }

    @Test
    void searchManagementInvocations_filtersByResolvedReleaseVersion() {
        Instant retentionAfter = Instant.parse("2026-06-15T11:00:00Z");
        Set<InvocationKind> kinds = EnumSet.of(
                InvocationKind.SINGLE,
                InvocationKind.BATCH_ROOT,
                InvocationKind.ASYNC_TASK
        );
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        repository.save(record(
                "INV-120",
                InvocationKind.SINGLE,
                "FAILED",
                CREDENTIAL_A,
                "version-request-120",
                now.minusSeconds(10),
                now.plusSeconds(3600),
                "1.2.0"
        ));

        AuditSearchPage<ApiInvocationRecordEntity> page = repository.searchManagementInvocations(
                TEMPLATE_ID,
                kinds,
                retentionAfter,
                null,
                null,
                null,
                null,
                null,
                null,
                "1.2.0",
                0,
                20
        );

        assertThat(page.content()).extracting(ApiInvocationRecordEntity::getInvocationExternalId)
                .containsExactly("INV-120");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void searchManagementInvocations_paginatesAndExcludesBatchItems() {
        Instant retentionAfter = Instant.parse("2026-06-15T11:00:00Z");
        Set<InvocationKind> kinds = EnumSet.of(
                InvocationKind.SINGLE,
                InvocationKind.BATCH_ROOT,
                InvocationKind.ASYNC_TASK
        );

        AuditSearchPage<ApiInvocationRecordEntity> firstPage = repository.searchManagementInvocations(
                TEMPLATE_ID,
                kinds,
                retentionAfter,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                2
        );
        AuditSearchPage<ApiInvocationRecordEntity> secondPage = repository.searchManagementInvocations(
                TEMPLATE_ID,
                kinds,
                retentionAfter,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                2
        );

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).extracting(ApiInvocationRecordEntity::getInvocationExternalId)
                .containsExactly("INV-003", "INV-002");
        assertThat(secondPage.content()).extracting(ApiInvocationRecordEntity::getInvocationExternalId)
                .containsExactly("INV-001");
    }

    private ApiInvocationRecordEntity record(
            String invocationExternalId,
            InvocationKind kind,
            String outcome,
            UUID credentialId,
            String requestId,
            Instant createdAt,
            Instant recordExpiresAt
    ) {
        return record(
                invocationExternalId,
                kind,
                outcome,
                credentialId,
                requestId,
                createdAt,
                recordExpiresAt,
                "1.0.0"
        );
    }

    private ApiInvocationRecordEntity record(
            String invocationExternalId,
            InvocationKind kind,
            String outcome,
            UUID credentialId,
            String requestId,
            Instant createdAt,
            Instant recordExpiresAt,
            String resolvedReleaseVersion
    ) {
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                invocationExternalId,
                kind,
                InvocationStatus.SUCCEEDED,
                "dev",
                TEMPLATE_ID,
                "TPL-001",
                credentialId,
                "svc-account-prod",
                requestId,
                "idem-" + invocationExternalId,
                "EXPLICIT_VERSION",
                resolvedReleaseVersion,
                resolvedReleaseVersion,
                "DOCX",
                "SYNC_STREAM",
                outcome,
                100L,
                "{\"variables\":{}}",
                null,
                null,
                false,
                recordExpiresAt,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                createdAt,
                createdAt
        );
    }
}
