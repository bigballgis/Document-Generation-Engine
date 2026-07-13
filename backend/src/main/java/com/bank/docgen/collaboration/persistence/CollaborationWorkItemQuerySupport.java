package com.bank.docgen.collaboration.persistence;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Package-private query helpers shared by {@link CollaborationWorkItemRepository} default methods.
 */
final class CollaborationWorkItemQuerySupport {

    private CollaborationWorkItemQuerySupport() {
    }

    static boolean isWildcardScope(Collection<String> groupCodes) {
        return groupCodes.size() == 1 && groupCodes.contains("*");
    }

    static EnumSet<CollaborationWorkItemQueue> escalationCandidateQueues() {
        return EnumSet.of(
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemQueue.APPROVAL,
                CollaborationWorkItemQueue.PENDING_RELEASE,
                CollaborationWorkItemQueue.REMEDIATION
        );
    }
}
