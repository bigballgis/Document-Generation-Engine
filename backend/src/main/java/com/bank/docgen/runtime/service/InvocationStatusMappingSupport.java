package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.domain.TaskStatus;

/**
 * Package-private status mapping for invocation records (pure functions).
 */
final class InvocationStatusMappingSupport {

    private InvocationStatusMappingSupport() {
    }

    static InvocationStatus mapOutcomeToStatus(String outcome) {
        if (RuntimeGenerationAuditRecorder.OUTCOME_FAILURE.equals(outcome)) {
            return InvocationStatus.FAILED;
        }
        return InvocationStatus.SUCCEEDED;
    }

    static InvocationStatus mapItemStatus(String itemStatus) {
        if ("FAILED".equalsIgnoreCase(itemStatus)) {
            return InvocationStatus.FAILED;
        }
        return InvocationStatus.SUCCEEDED;
    }

    static InvocationStatus mapBatchRootStatus(BatchResultView batchResult) {
        boolean anyFailed = batchResult.items().stream()
                .anyMatch(item -> "FAILED".equalsIgnoreCase(item.status()));
        boolean anySucceeded = batchResult.items().stream()
                .anyMatch(item -> "SUCCEEDED".equalsIgnoreCase(item.status()));
        if (anyFailed && anySucceeded) {
            return InvocationStatus.PARTIAL_SUCCEEDED;
        }
        if (anyFailed) {
            return InvocationStatus.FAILED;
        }
        return InvocationStatus.SUCCEEDED;
    }

    static InvocationStatus mapTaskStatus(TaskStatus taskStatus) {
        return switch (taskStatus) {
            case ACCEPTED -> InvocationStatus.ACCEPTED;
            case PROCESSING -> InvocationStatus.PROCESSING;
            case SUCCEEDED -> InvocationStatus.SUCCEEDED;
            case FAILED -> InvocationStatus.FAILED;
            case PARTIAL_SUCCEEDED -> InvocationStatus.PARTIAL_SUCCEEDED;
            case EXPIRED -> InvocationStatus.EXPIRED;
            case CANCELLED -> InvocationStatus.CANCELLED;
        };
    }
}
