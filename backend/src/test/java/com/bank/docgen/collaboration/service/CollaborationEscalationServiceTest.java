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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationEscalationServiceTest {

    private static final UUID WORK_ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-06-26T12:00:00Z");

    @Mock
    private CollaborationWorkItemRepository workItemRepository;
    @Mock
    private CollaborationTimeoutResolver timeoutResolver;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private CollaborationEscalationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CollaborationEscalationService(
                workItemRepository,
                timeoutResolver,
                auditRecorder,
                clock
        );
    }

    @Test
    void processDueEscalations_createsEscalationWhenTestTodoExceedsThreshold() {
        CollaborationWorkItemEntity overdue = overdueTestItem(NOW.minusSeconds(73 * 3600L));
        when(workItemRepository.findOpenEscalationCandidates()).thenReturn(List.of(overdue));
        when(timeoutResolver.resolveForGroup("RETAIL")).thenReturn(defaultThresholds(72));
        when(workItemRepository.existsOpenEscalationForSource(WORK_ITEM_ID)).thenReturn(false);

        int created = service.processDueEscalations();

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<CollaborationWorkItemEntity> saved = ArgumentCaptor.forClass(CollaborationWorkItemEntity.class);
        verify(workItemRepository).save(saved.capture());
        CollaborationWorkItemEntity escalation = saved.getValue();
        assertThat(escalation.getQueue()).isEqualTo(CollaborationWorkItemQueue.ESCALATION);
        assertThat(escalation.getTriggerType()).isEqualTo(CollaborationWorkItemTriggerType.TIMEOUT_ESCALATION);
        assertThat(escalation.getStatus()).isEqualTo(CollaborationWorkItemStatus.OPEN);
        assertThat(escalation.getSourceWorkItemId()).isEqualTo(WORK_ITEM_ID);
        assertThat(escalation.getTemplateId()).isEqualTo(TEMPLATE_ID);
        verify(auditRecorder).recordCollaborationTimeoutEscalation(
                eq(TEMPLATE_ID),
                eq("RETAIL"),
                eq(WORK_ITEM_ID),
                eq(CollaborationWorkItemQueue.TEST),
                any()
        );
    }

    @Test
    void processDueEscalations_skipsWhenWithinThreshold() {
        CollaborationWorkItemEntity recent = overdueTestItem(NOW.minusSeconds(24 * 3600L));
        when(workItemRepository.findOpenEscalationCandidates()).thenReturn(List.of(recent));
        when(timeoutResolver.resolveForGroup("RETAIL")).thenReturn(defaultThresholds(72));

        int created = service.processDueEscalations();

        assertThat(created).isZero();
        verify(workItemRepository, never()).save(any());
        verify(auditRecorder, never()).recordCollaborationTimeoutEscalation(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void processDueEscalations_doesNotDuplicateExistingEscalation() {
        CollaborationWorkItemEntity overdue = overdueTestItem(NOW.minusSeconds(80 * 3600L));
        when(workItemRepository.findOpenEscalationCandidates()).thenReturn(List.of(overdue));
        when(timeoutResolver.resolveForGroup("RETAIL")).thenReturn(defaultThresholds(72));
        when(workItemRepository.existsOpenEscalationForSource(WORK_ITEM_ID)).thenReturn(true);

        int created = service.processDueEscalations();

        assertThat(created).isZero();
        verify(workItemRepository, never()).save(any());
        verify(auditRecorder, never()).recordCollaborationTimeoutEscalation(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void processDueEscalations_doesNotMutateSourceWorkItem() {
        CollaborationWorkItemEntity overdue = overdueTestItem(NOW.minusSeconds(80 * 3600L));
        when(workItemRepository.findOpenEscalationCandidates()).thenReturn(List.of(overdue));
        when(timeoutResolver.resolveForGroup("RETAIL")).thenReturn(defaultThresholds(72));
        when(workItemRepository.existsOpenEscalationForSource(WORK_ITEM_ID)).thenReturn(false);

        service.processDueEscalations();

        assertThat(overdue.getStatus()).isEqualTo(CollaborationWorkItemStatus.OPEN);
        assertThat(overdue.getQueue()).isEqualTo(CollaborationWorkItemQueue.TEST);
        assertThat(overdue.getResolvedAt()).isNull();
    }

    @Test
    void processDueEscalations_usesGroupOverrideThreshold() {
        CollaborationWorkItemEntity overdue = overdueTestItem(NOW.minusSeconds(25 * 3600L));
        when(workItemRepository.findOpenEscalationCandidates()).thenReturn(List.of(overdue));
        when(timeoutResolver.resolveForGroup("RETAIL"))
                .thenReturn(new CollaborationTimeoutConfigView("GROUP", "RETAIL", 24, 72, 48, 168, null));
        when(workItemRepository.existsOpenEscalationForSource(WORK_ITEM_ID)).thenReturn(false);

        int created = service.processDueEscalations();

        assertThat(created).isEqualTo(1);
        verify(workItemRepository).save(any(CollaborationWorkItemEntity.class));
    }

    private CollaborationWorkItemEntity overdueTestItem(Instant createdAt) {
        CollaborationWorkItemEntity entity = new CollaborationWorkItemEntity(
                WORK_ITEM_ID,
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Loan Notice Template",
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                "10000003",
                "Template submitted for testing"
        );
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private CollaborationTimeoutConfigView defaultThresholds(int testHours) {
        return new CollaborationTimeoutConfigView("GLOBAL", null, testHours, 72, 48, 168, null);
    }
}
