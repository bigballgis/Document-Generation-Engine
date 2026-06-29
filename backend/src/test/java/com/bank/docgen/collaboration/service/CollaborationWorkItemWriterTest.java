package com.bank.docgen.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationWorkItemWriterTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID WORK_ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String SUMMARY = "Template submitted for testing";
    private static final String REMEDIATION_SUMMARY = "Template returned to drafting for fixes after test";

    @Mock
    private CollaborationWorkItemRepository workItemRepository;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private CollaborationWorkItemWriter writer;
    private ManagementSessionClaims author;

    @BeforeEach
    void setUp() {
        writer = new CollaborationWorkItemWriter(workItemRepository, messageResolver, auditRecorder);
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
        lenient().when(messageResolver.resolve(CollaborationWorkItemWriter.SUBMIT_FOR_TEST_SUMMARY_KEY))
                .thenReturn(SUMMARY);
        lenient().when(messageResolver.resolve(CollaborationWorkItemWriter.REMEDIATION_SUMMARY_KEY))
                .thenReturn(REMEDIATION_SUMMARY);
    }

    @Test
    void resolveOpenTestWorkItems_resolvesAllOpenItemsAndRecordsResolveAudit() {
        TemplateEntity template = draftTemplate();
        CollaborationWorkItemEntity first = openTestItem(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), "10000001");
        CollaborationWorkItemEntity second = openTestItem(
                UUID.fromString("22222222-2222-2222-2222-222222222222"), "10000001");
        when(workItemRepository.findAllOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.TEST))
                .thenReturn(List.of(first, second));
        when(workItemRepository.save(any(CollaborationWorkItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<String> carriedSubmitter = writer.resolveOpenTestWorkItems(template, author);

        assertThat(carriedSubmitter).contains("10000001");
        assertThat(first.getStatus()).isEqualTo(CollaborationWorkItemStatus.RESOLVED);
        assertThat(first.getResolvedAt()).isNotNull();
        assertThat(second.getStatus()).isEqualTo(CollaborationWorkItemStatus.RESOLVED);
        verify(workItemRepository, times(2)).save(any(CollaborationWorkItemEntity.class));
        verify(auditRecorder, times(2)).recordCollaborationWorkItemResolved(
                eq(TEMPLATE_ID), eq("RETAIL"), any(UUID.class), eq(CollaborationWorkItemQueue.TEST),
                eq("10000003"), any());
    }

    @Test
    void resolveOpenTestWorkItems_noOpenItems_isIdempotentNoOp() {
        TemplateEntity template = draftTemplate();
        when(workItemRepository.findAllOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.TEST))
                .thenReturn(List.of());

        Optional<String> carriedSubmitter = writer.resolveOpenTestWorkItems(template, author);

        assertThat(carriedSubmitter).isEmpty();
        verify(workItemRepository, never()).save(any(CollaborationWorkItemEntity.class));
        verify(auditRecorder, never()).recordCollaborationWorkItemResolved(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void upsertRemediationWorkItem_createsOpenRemediationWithCarryForwardSubmitterAndCreateAudit() {
        TemplateEntity template = draftTemplate();
        when(workItemRepository.findOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.REMEDIATION))
                .thenReturn(Optional.empty());
        ArgumentCaptor<CollaborationWorkItemEntity> captor =
                ArgumentCaptor.forClass(CollaborationWorkItemEntity.class);
        when(workItemRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        CollaborationWorkItemEntity saved = writer.upsertRemediationWorkItem(template, "10000007", author);

        assertThat(saved.getQueue()).isEqualTo(CollaborationWorkItemQueue.REMEDIATION);
        assertThat(saved.getTriggerType())
                .isEqualTo(CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT);
        assertThat(saved.getStatus()).isEqualTo(CollaborationWorkItemStatus.OPEN);
        assertThat(saved.getGroupCode()).isEqualTo("RETAIL");
        assertThat(saved.getSubmitterUserId()).isEqualTo("10000007");
        assertThat(saved.getSummaryText()).isEqualTo(REMEDIATION_SUMMARY);
        assertThat(saved.getSummaryText()).doesNotContain("Ready for test");
        verify(auditRecorder).recordCollaborationWorkItemCreated(
                eq(TEMPLATE_ID), eq("RETAIL"), any(UUID.class), eq(CollaborationWorkItemQueue.REMEDIATION),
                eq(CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT), eq("10000003"), any());
    }

    @Test
    void upsertRemediationWorkItem_refreshesExistingOpenRemediationIdempotently() {
        TemplateEntity template = draftTemplate();
        CollaborationWorkItemEntity existing = new CollaborationWorkItemEntity(
                WORK_ITEM_ID,
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Old Name",
                "RETAIL",
                CollaborationWorkItemQueue.REMEDIATION,
                CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT,
                CollaborationWorkItemStatus.OPEN,
                "10000001",
                "Old summary"
        );
        when(workItemRepository.findOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.REMEDIATION))
                .thenReturn(Optional.of(existing));
        when(workItemRepository.save(existing)).thenReturn(existing);

        CollaborationWorkItemEntity saved = writer.upsertRemediationWorkItem(template, "10000007", author);

        assertThat(saved.getId()).isEqualTo(WORK_ITEM_ID);
        assertThat(saved.getSubmitterUserId()).isEqualTo("10000007");
        assertThat(saved.getSummaryText()).isEqualTo(REMEDIATION_SUMMARY);
        verify(workItemRepository, times(1)).save(existing);
        verify(auditRecorder, never()).recordCollaborationWorkItemCreated(
                any(), any(), any(), any(), any(), any(), any());
    }

    private CollaborationWorkItemEntity openTestItem(UUID id, String submitter) {
        return new CollaborationWorkItemEntity(
                id,
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Loan Notice Template",
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                submitter,
                "Non-sensitive summary"
        );
    }

    @Test
    void upsertSubmitForTestWorkItem_createsOpenTestQueueItem() {
        TemplateEntity template = draftTemplate();
        when(workItemRepository.findOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.TEST))
                .thenReturn(Optional.empty());
        when(workItemRepository.save(any(CollaborationWorkItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CollaborationWorkItemEntity saved = writer.upsertSubmitForTestWorkItem(template, author);

        assertThat(saved.getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(saved.getTemplateExternalId()).isEqualTo("TPL-LOAN-NOTICE");
        assertThat(saved.getTemplateName()).isEqualTo("Loan Notice Template");
        assertThat(saved.getGroupCode()).isEqualTo("RETAIL");
        assertThat(saved.getQueue()).isEqualTo(CollaborationWorkItemQueue.TEST);
        assertThat(saved.getTriggerType()).isEqualTo(CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST);
        assertThat(saved.getStatus()).isEqualTo(CollaborationWorkItemStatus.OPEN);
        assertThat(saved.getSubmitterUserId()).isEqualTo("10000003");
        assertThat(saved.getSummaryText()).isEqualTo(SUMMARY);
    }

    @Test
    void upsertSubmitForTestWorkItem_refreshesExistingOpenItemIdempotently() {
        TemplateEntity template = draftTemplate();
        CollaborationWorkItemEntity existing = new CollaborationWorkItemEntity(
                WORK_ITEM_ID,
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Old Name",
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                "10000001",
                "Old summary"
        );
        when(workItemRepository.findOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.TEST))
                .thenReturn(Optional.of(existing));
        when(workItemRepository.save(existing)).thenReturn(existing);

        CollaborationWorkItemEntity saved = writer.upsertSubmitForTestWorkItem(template, author);

        assertThat(saved.getId()).isEqualTo(WORK_ITEM_ID);
        assertThat(saved.getTemplateName()).isEqualTo("Loan Notice Template");
        assertThat(saved.getSubmitterUserId()).isEqualTo("10000003");
        assertThat(saved.getSummaryText()).isEqualTo(SUMMARY);
        verify(workItemRepository, times(1)).save(existing);
    }

    @Test
    void upsertSubmitForTestWorkItem_doesNotPersistCommentSummary() {
        TemplateEntity template = draftTemplate();
        when(workItemRepository.findOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.TEST))
                .thenReturn(Optional.empty());
        ArgumentCaptor<CollaborationWorkItemEntity> captor = ArgumentCaptor.forClass(CollaborationWorkItemEntity.class);
        when(workItemRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.upsertSubmitForTestWorkItem(template, author);

        assertThat(captor.getValue().getSummaryText()).isEqualTo(SUMMARY);
        assertThat(captor.getValue().getSummaryText()).doesNotContain("Ready for test");
    }

    private TemplateEntity draftTemplate() {
        TemplateEntity entity = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "RETAIL",
                "Loan Notice Template",
                null,
                UUID.randomUUID(),
                "10000003"
        );
        entity.setLifecycleStatus(com.bank.docgen.template.domain.TemplateLifecycleStatus.DRAFT);
        return entity;
    }
}
