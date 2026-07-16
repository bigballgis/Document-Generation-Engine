package com.bank.docgen.legalhold.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import com.bank.docgen.legalhold.persistence.LegalHoldRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-G04-007…014 exemption rules (G04-C5…C15).
 */
@ExtendWith(MockitoExtension.class)
class LegalHoldExemptionServiceTest {

    private static final UUID TEMPLATE_T = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
    private static final UUID TEMPLATE_OTHER = UUID.fromString("ffffffff-1111-4222-8333-444444444444");
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-30T23:59:59Z");
    private static final Instant IN_WINDOW = Instant.parse("2026-03-15T12:00:00Z");
    private static final Instant BEFORE_WINDOW = Instant.parse("2025-12-31T23:59:59Z");
    private static final Instant AFTER_WINDOW = Instant.parse("2026-07-01T00:00:00Z");

    @Mock
    private LegalHoldRepository repository;

    private LegalHoldExemptionService service;

    @BeforeEach
    void setUp() {
        service = new LegalHoldExemptionService(repository);
    }

    @Test
    void invocation_templateWindow_exemptsMatchingCreatedAt() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(TO)));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", IN_WINDOW)).isTrue();
        assertThat(service.isInvocationExempt(TEMPLATE_OTHER, "INV-1", IN_WINDOW)).isFalse();
        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", BEFORE_WINDOW)).isFalse();
        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", AFTER_WINDOW)).isFalse();
        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", FROM)).isTrue();
        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", TO)).isTrue();
    }

    @Test
    void invocation_templateWindow_openEnded_toNull() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(null)));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", AFTER_WINDOW)).isTrue();
    }

    @Test
    void invocation_invocationSet_exemptsListedId() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("INV-PROTECTED", "INV-2"))));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-PROTECTED", IN_WINDOW)).isTrue();
        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-OTHER", IN_WINDOW)).isFalse();
    }

    @Test
    void releasedHold_neverExempts() {
        LegalHoldEntity released = templateWindowHold(TO);
        released.release(Instant.now(), "10000001");
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of());

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", IN_WINDOW)).isFalse();
    }

    @Test
    void managementAudit_templateWindow_requiresTemplateIdAndWindow() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(TO)));

        assertThat(service.isManagementAuditExempt(TEMPLATE_T, IN_WINDOW)).isTrue();
        assertThat(service.isManagementAuditExempt(null, IN_WINDOW)).isFalse();
        assertThat(service.isManagementAuditExempt(TEMPLATE_OTHER, IN_WINDOW)).isFalse();
    }

    @Test
    void managementAudit_invocationSet_neverExempts() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("INV-1"))));

        assertThat(service.isManagementAuditExempt(TEMPLATE_T, IN_WINDOW)).isFalse();
    }

    @Test
    void runtimeAudit_templateWindow_matches() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(TO)));

        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, IN_WINDOW, null, null)).isTrue();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_OTHER, IN_WINDOW, null, null)).isFalse();
    }

    @Test
    void runtimeAudit_invocationSet_matchesTaskOrDocumentId() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("inv-1"))));

        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, IN_WINDOW, "inv-1", null)).isTrue();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, IN_WINDOW, null, "inv-1")).isTrue();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, IN_WINDOW, "other", "other")).isFalse();
    }

    private LegalHoldEntity templateWindowHold(Instant effectiveTo) {
        return new LegalHoldEntity(
                UUID.randomUUID(),
                "HOLD-TW-1",
                LegalHoldScopeType.TEMPLATE_WINDOW,
                LegalHoldStatus.ACTIVE,
                "litigation",
                TEMPLATE_T,
                "TPL-001",
                FROM,
                effectiveTo,
                Instant.parse("2026-01-02T00:00:00Z"),
                "10000001",
                Set.of()
        );
    }

    private LegalHoldEntity invocationSetHold(Set<String> ids) {
        return new LegalHoldEntity(
                UUID.randomUUID(),
                "HOLD-IS-1",
                LegalHoldScopeType.INVOCATION_SET,
                LegalHoldStatus.ACTIVE,
                "preserve",
                null,
                null,
                null,
                null,
                Instant.parse("2026-01-02T00:00:00Z"),
                "10000001",
                ids
        );
    }
}
