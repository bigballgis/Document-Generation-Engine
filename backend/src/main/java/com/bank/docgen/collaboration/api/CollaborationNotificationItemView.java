package com.bank.docgen.collaboration.api;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import java.time.Instant;

/**
 * Projection of an OPEN collaboration work item for the in-app notification center.
 * {@code queue} supports deep-link construction: {@code /dashboard?queue={QUEUE}#tasks-section}.
 */
public record CollaborationNotificationItemView(
        String workItemId,
        String templateId,
        String templateName,
        String groupCode,
        CollaborationWorkItemQueue queue,
        CollaborationWorkItemTriggerType triggerType,
        String summaryText,
        Instant createdAt,
        long ageSeconds,
        boolean read
) {
}
