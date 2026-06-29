package com.bank.docgen.collaboration.service;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationWorkItemWriter {

    static final String SUBMIT_FOR_TEST_SUMMARY_KEY = "api.collaboration.workItem.submitForTest.summary";

    private final CollaborationWorkItemRepository workItemRepository;
    private final MessageResolver messageResolver;

    public CollaborationWorkItemWriter(
            CollaborationWorkItemRepository workItemRepository,
            MessageResolver messageResolver
    ) {
        this.workItemRepository = workItemRepository;
        this.messageResolver = messageResolver;
    }

    @Transactional
    public CollaborationWorkItemEntity upsertSubmitForTestWorkItem(
            TemplateEntity template,
            ManagementSessionClaims session
    ) {
        String summary = messageResolver.resolve(SUBMIT_FOR_TEST_SUMMARY_KEY);
        Instant now = Instant.now();
        return workItemRepository
                .findOpenByTemplateIdAndQueue(template.getId(), CollaborationWorkItemQueue.TEST)
                .map(existing -> refreshSubmitForTest(existing, template, session, summary, now))
                .orElseGet(() -> createSubmitForTest(template, session, summary, now));
    }

    private CollaborationWorkItemEntity refreshSubmitForTest(
            CollaborationWorkItemEntity existing,
            TemplateEntity template,
            ManagementSessionClaims session,
            String summary,
            Instant now
    ) {
        existing.setTemplateName(template.getName());
        existing.setSubmitterUserId(session.username());
        existing.setSummaryText(summary);
        existing.setTriggerType(CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST);
        existing.setCreatedAt(now);
        existing.setUpdatedAt(now);
        return workItemRepository.save(existing);
    }

    private CollaborationWorkItemEntity createSubmitForTest(
            TemplateEntity template,
            ManagementSessionClaims session,
            String summary,
            Instant now
    ) {
        CollaborationWorkItemEntity created = new CollaborationWorkItemEntity(
                UUID.randomUUID(),
                template.getId(),
                template.getExternalId(),
                template.getName(),
                template.getGroupCode(),
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                session.username(),
                summary
        );
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        return workItemRepository.save(created);
    }
}
