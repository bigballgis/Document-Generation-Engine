package com.bank.docgen.collaboration.api;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import java.time.Instant;

public record CollaborationWorkItemSummaryView(
        String workItemId,
        String templateId,
        String templateName,
        String groupCode,
        CollaborationWorkItemQueue queue,
        CollaborationWorkItemTriggerType triggerType,
        String submitterUserId,
        String summaryText,
        Instant createdAt,
        long ageSeconds
) {
}
