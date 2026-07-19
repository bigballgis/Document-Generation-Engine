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
 * IBL-D5 / F23 — enforce (exemption hit) + block (non-exempt / released / out-of-window / mismatch)
 * regression matrix for CE-G04-C5…C15.
 */
@ExtendWith(MockitoExtension.class)
class LegalHoldEnforceBlockMatrixTest {

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
    void enforce_templateWindow_hitsInvocationAndBothAuditSurfaces() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(TO)));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-ANY", IN_WINDOW)).isTrue();
        assertThat(service.isManagementAuditExempt(TEMPLATE_T, IN_WINDOW)).isTrue();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, IN_WINDOW, "task-x", "doc-x")).isTrue();
    }

    @Test
    void enforce_invocationSet_hitsInvocationAndRuntimeAuditByTaskOrDocument() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("INV-PROTECTED"))));

        assertThat(service.isInvocationExempt(TEMPLATE_OTHER, "INV-PROTECTED", AFTER_WINDOW)).isTrue();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, AFTER_WINDOW, "INV-PROTECTED", null)).isTrue();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, AFTER_WINDOW, null, "INV-PROTECTED")).isTrue();
    }

    @Test
    void enforce_anyOfMultipleActiveHolds_isSufficient() {
        LegalHoldEntity unrelated = templateWindowHold(TO);
        LegalHoldEntity matching = invocationSetHold(Set.of("INV-HIT"));
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(unrelated, matching));

        assertThat(service.isInvocationExempt(TEMPLATE_OTHER, "INV-HIT", BEFORE_WINDOW)).isTrue();
    }

    @Test
    void block_scopeMismatch_neverExempts() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(TO)));

        assertThat(service.isInvocationExempt(TEMPLATE_OTHER, "INV-1", IN_WINDOW)).isFalse();
        assertThat(service.isManagementAuditExempt(TEMPLATE_OTHER, IN_WINDOW)).isFalse();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_OTHER, IN_WINDOW, null, null)).isFalse();
    }

    @Test
    void block_outOfWindow_neverExemptsClosedOrOpenEndedEdges() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(TO)));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", BEFORE_WINDOW)).isFalse();
        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", AFTER_WINDOW)).isFalse();
        assertThat(service.isManagementAuditExempt(TEMPLATE_T, BEFORE_WINDOW)).isFalse();
        assertThat(service.isManagementAuditExempt(TEMPLATE_T, AFTER_WINDOW)).isFalse();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, AFTER_WINDOW, null, null)).isFalse();
    }

    @Test
    void block_releasedHold_neverExemptsEvenWhenQueriedOnlyActive() {
        // G04-C6 critical regression: RELEASED must not appear in ACTIVE set; empty ACTIVE ⇒ no exemption.
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of());

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", IN_WINDOW)).isFalse();
        assertThat(service.isManagementAuditExempt(TEMPLATE_T, IN_WINDOW)).isFalse();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, IN_WINDOW, "INV-1", "INV-1")).isFalse();
    }

    @Test
    void block_invocationSet_doesNotProtectManagementAudit() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("INV-1"))));

        assertThat(service.isManagementAuditExempt(TEMPLATE_T, IN_WINDOW)).isFalse();
    }

    @Test
    void block_nullCreatedAtOrEventAt_neverExemptsTemplateWindow() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(templateWindowHold(TO)));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", null)).isFalse();
        assertThat(service.isManagementAuditExempt(TEMPLATE_T, null)).isFalse();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, null, null, null)).isFalse();
    }

    @Test
    void block_nullTemplateId_neverExemptsManagementAudit() {
        // Fail-closed short-circuit before repository lookup (G04-C12 platform rows).
        assertThat(service.isManagementAuditExempt(null, IN_WINDOW)).isFalse();
    }

    @Test
    void block_invocationSet_unknownId_neverExempts() {
        when(repository.findByStatus(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(invocationSetHold(Set.of("INV-PROTECTED"))));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-OTHER", IN_WINDOW)).isFalse();
        assertThat(service.isInvocationExempt(TEMPLATE_T, null, IN_WINDOW)).isFalse();
        assertThat(service.isRuntimeAuditExempt(TEMPLATE_T, IN_WINDOW, "other", "other")).isFalse();
    }

    @Test
    void block_templateWindowWithNullEffectiveFrom_neverExempts() {
        LegalHoldEntity broken = new LegalHoldEntity(
                UUID.randomUUID(),
                "HOLD-BROKEN",
                LegalHoldScopeType.TEMPLATE_WINDOW,
                LegalHoldStatus.ACTIVE,
                null,
                TEMPLATE_T,
                "TPL-001",
                null,
                TO,
                Instant.parse("2026-01-02T00:00:00Z"),
                "10000001",
                Set.of()
        );
        when(repository.findByStatus(LegalHoldStatus.ACTIVE)).thenReturn(List.of(broken));

        assertThat(service.isInvocationExempt(TEMPLATE_T, "INV-1", IN_WINDOW)).isFalse();
        assertThat(service.isManagementAuditExempt(TEMPLATE_T, IN_WINDOW)).isFalse();
    }

    private LegalHoldEntity templateWindowHold(Instant effectiveTo) {
        return new LegalHoldEntity(
                UUID.randomUUID(),
                "HOLD-TW-MATRIX",
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
                "HOLD-IS-MATRIX",
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
