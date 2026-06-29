package com.bank.docgen.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.collaboration.api.CollaborationTimeoutConfigView;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.infrastructure.config.QuerydslConfig;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({QuerydslConfig.class, CollaborationEscalationService.class})
@ActiveProfiles("test")
class CollaborationEscalationServiceDataJpaTest {

    private static final UUID SOURCE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-06-26T12:00:00Z");

    @Autowired
    private CollaborationWorkItemRepository workItemRepository;

    @Autowired
    private CollaborationEscalationService escalationService;

    @MockBean
    private CollaborationTimeoutResolver timeoutResolver;

    @MockBean
    private ManagementAuditRecorder auditRecorder;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(timeoutResolver.resolveForGroup("RETAIL"))
                .thenReturn(new CollaborationTimeoutConfigView("GLOBAL", null, 72, 72, 48, 168, null));
    }

    @Test
    void processDueEscalations_persistsEscalationLinkedToSource() {
        CollaborationWorkItemEntity source = overdueApprovalItem();
        workItemRepository.save(source);

        int created = escalationService.processDueEscalations();

        assertThat(created).isEqualTo(1);
        assertThat(workItemRepository.existsOpenEscalationForSource(SOURCE_ID)).isTrue();
        assertThat(workItemRepository.findById(SOURCE_ID))
                .isPresent()
                .get()
                .satisfies(item -> {
                    assertThat(item.getStatus()).isEqualTo(CollaborationWorkItemStatus.OPEN);
                    assertThat(item.getQueue()).isEqualTo(CollaborationWorkItemQueue.APPROVAL);
                });
        verify(auditRecorder).recordCollaborationTimeoutEscalation(
                eq(TEMPLATE_ID),
                eq("RETAIL"),
                eq(SOURCE_ID),
                eq(CollaborationWorkItemQueue.APPROVAL),
                any()
        );
    }

    @Test
    void processDueEscalations_isIdempotentAcrossRuns() {
        workItemRepository.save(overdueApprovalItem());

        assertThat(escalationService.processDueEscalations()).isEqualTo(1);
        assertThat(escalationService.processDueEscalations()).isZero();
        assertThat(workItemRepository.findOpenEscalationCandidates()).hasSize(1);
    }

    @Test
    void processDueEscalations_skipsWhenNotOverdue() {
        CollaborationWorkItemEntity recent = overdueApprovalItem();
        recent.setCreatedAt(NOW.minusSeconds(24 * 3600L));
        workItemRepository.save(recent);

        int created = escalationService.processDueEscalations();

        assertThat(created).isZero();
        verify(auditRecorder, never()).recordCollaborationTimeoutEscalation(
                any(), any(), any(), any(), any()
        );
    }

    private CollaborationWorkItemEntity overdueApprovalItem() {
        CollaborationWorkItemEntity entity = new CollaborationWorkItemEntity(
                SOURCE_ID,
                TEMPLATE_ID,
                "TPL-001",
                "Loan Notice Template",
                "RETAIL",
                CollaborationWorkItemQueue.APPROVAL,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL,
                CollaborationWorkItemStatus.OPEN,
                "10000003",
                "Awaiting approval"
        );
        entity.setCreatedAt(NOW.minusSeconds(80 * 3600L));
        return entity;
    }
}
