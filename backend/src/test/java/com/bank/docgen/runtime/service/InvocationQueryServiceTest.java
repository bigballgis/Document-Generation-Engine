package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.runtime.api.InvocationDetailView;
import com.bank.docgen.runtime.api.InvocationListResultView;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationListView;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.domain.InvocationViewValidationException;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InvocationQueryServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CREDENTIAL_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER_CREDENTIAL_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private ApiInvocationRecordRepository repository;

    private InvocationQueryService service;
    private TemplateEntity template;
    private RuntimeSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new InvocationQueryService(repository, new ObjectMapper());
        template = new TemplateEntity(TEMPLATE_ID, "TPL-001", "GRP", "Demo", null, null, "U0000001");
        session = new RuntimeSessionClaims(
                CREDENTIAL_ID,
                "CRED-001",
                TEMPLATE_ID,
                "TPL-001",
                "svc-account",
                List.of("grp-a")
        );
    }

    @Test
    void listInvocations_logicalViewReturnsRootKindsOnly() {
        ApiInvocationRecordEntity root = record("INV-ROOT01", InvocationKind.BATCH_ROOT, CREDENTIAL_ID);
        when(repository.findByTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                eq(TEMPLATE_ID),
                eq(CREDENTIAL_ID),
                eq(Set.of(InvocationKind.SINGLE, InvocationKind.BATCH_ROOT, InvocationKind.ASYNC_TASK)),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(root)));
        when(repository.countByBatchExternalIdAndInvocationKindAndCredentialId(
                "BATCH-001",
                InvocationKind.BATCH_ITEM,
                CREDENTIAL_ID
        )).thenReturn(2L);

        InvocationListResultView result = service.listInvocations(template, session, "logical", null, 0, 20);

        assertThat(result.view()).isEqualTo("logical");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().invocationKind()).isEqualTo("BATCH_ROOT");
        assertThat(result.items().getFirst().childItemCount()).isEqualTo(2);
    }

    @Test
    void listInvocations_flatViewUsesFlatKinds() {
        ApiInvocationRecordEntity item = record("INV-ITEM01", InvocationKind.BATCH_ITEM, CREDENTIAL_ID);
        when(repository.findByTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                eq(TEMPLATE_ID),
                eq(CREDENTIAL_ID),
                eq(Set.of(InvocationKind.SINGLE, InvocationKind.BATCH_ITEM)),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(item)));

        InvocationListResultView result = service.listInvocations(template, session, "flat", null, 0, 20);

        assertThat(result.view()).isEqualTo("flat");
        assertThat(result.items().getFirst().invocationKind()).isEqualTo("BATCH_ITEM");
    }

    @Test
    void listInvocations_invalidViewThrows() {
        assertThatThrownBy(() -> service.listInvocations(template, session, "broken", null, 0, 20))
                .isInstanceOf(InvocationViewValidationException.class);
    }

    @Test
    void getInvocationDetail_crossCredentialDenied() {
        ApiInvocationRecordEntity record = record("INV-OTHER", InvocationKind.SINGLE, OTHER_CREDENTIAL_ID);
        when(repository.findByInvocationExternalId("INV-OTHER")).thenReturn(java.util.Optional.of(record));

        assertThatThrownBy(() -> service.getInvocationDetail(template, session, "INV-OTHER"))
                .isInstanceOf(RuntimeAccessDeniedException.class);
    }

    @Test
    void getInvocationDetail_expiredRecordThrows() {
        ApiInvocationRecordEntity expired = expiredRecord("INV-EXPIRED", CREDENTIAL_ID);
        when(repository.findByInvocationExternalId("INV-EXPIRED")).thenReturn(java.util.Optional.of(expired));

        assertThatThrownBy(() -> service.getInvocationDetail(template, session, "INV-EXPIRED"))
                .isInstanceOf(InvocationRecordExpiredException.class);
    }

    @Test
    void getInvocationDetail_returnsSanitizedParameters() {
        ApiInvocationRecordEntity record = recordWithParameters(
                "INV-DETAIL",
                CREDENTIAL_ID,
                "{\"variables\":{\"name\":\"Alice\"},\"encryption\":{\"enabled\":true,\"openPasswordProvided\":true}}"
        );
        when(repository.findByInvocationExternalId("INV-DETAIL")).thenReturn(java.util.Optional.of(record));

        InvocationDetailView detail = service.getInvocationDetail(template, session, "INV-DETAIL");

        assertThat(detail.parameters()).containsEntry("variables", java.util.Map.of("name", "Alice"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> encryption =
                (java.util.Map<String, Object>) detail.parameters().get("encryption");
        assertThat(encryption).doesNotContainKey("openPassword");
        assertThat(encryption).doesNotContainKey("ownerPassword");
        assertThat(encryption.get("openPasswordProvided")).isEqualTo(true);
    }

    private ApiInvocationRecordEntity record(String externalId, InvocationKind kind, UUID credentialId) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                kind,
                InvocationStatus.SUCCEEDED,
                "dev",
                TEMPLATE_ID,
                "TPL-001",
                credentialId,
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
                null,
                null,
                false,
                now.plusSeconds(3600),
                null,
                "BATCH-001",
                null,
                null,
                null,
                null,
                "audit-1",
                kind == InvocationKind.BATCH_ITEM || kind == InvocationKind.BATCH_ROOT,
                now,
                now
        );
    }

    private ApiInvocationRecordEntity recordWithParameters(
            String externalId,
            UUID credentialId,
            String parameters
    ) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                TEMPLATE_ID,
                "TPL-001",
                credentialId,
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
                parameters,
                "DOC-1",
                null,
                true,
                now.plusSeconds(3600),
                now.plusSeconds(1800),
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                false,
                now,
                now
        );
    }

    private ApiInvocationRecordEntity expiredRecord(String externalId, UUID credentialId) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                TEMPLATE_ID,
                "TPL-001",
                credentialId,
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
                null,
                null,
                false,
                now.minusSeconds(60),
                null,
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                false,
                now.minusSeconds(120),
                now.minusSeconds(60)
        );
    }
}
