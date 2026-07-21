package com.bank.docgen.collaboration.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CollaborationWorkItemAccessService {

    private final GroupAccessService groupAccessService;

    public CollaborationWorkItemAccessService(GroupAccessService groupAccessService) {
        this.groupAccessService = groupAccessService;
    }

    public void requireViewer(ManagementSessionClaims session) {
        if (!groupAccessService.canViewCollaborationWorkItems(session)) {
            throw new CollaborationWorkItemAccessDeniedException();
        }
    }

    public Set<CollaborationWorkItemQueue> visibleQueues(ManagementSessionClaims session) {
        requireViewer(session);
        if (groupAccessService.hasCollaborationWorkItemAdminVisibility(session)) {
            return EnumSet.allOf(CollaborationWorkItemQueue.class);
        }
        EnumSet<CollaborationWorkItemQueue> queues = EnumSet.noneOf(CollaborationWorkItemQueue.class);
        List<String> roles = session.roles();
        if (roles.contains("TEMPLATE_TESTER")) {
            queues.add(CollaborationWorkItemQueue.TEST);
        }
        if (roles.contains("LEGAL_REVIEWER")) {
            queues.add(CollaborationWorkItemQueue.LEGAL);
        }
        if (roles.contains("DOCUMENT_AUTHOR")) {
            queues.add(CollaborationWorkItemQueue.REMEDIATION);
            queues.add(CollaborationWorkItemQueue.PENDING_RELEASE);
        }
        return queues;
    }

    public void requireVisibleQueue(ManagementSessionClaims session, CollaborationWorkItemQueue queue) {
        if (!visibleQueues(session).contains(queue)) {
            throw new CollaborationWorkItemAccessDeniedException();
        }
    }
}
