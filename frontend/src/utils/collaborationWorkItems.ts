import { templateLifecyclePanelPath } from '@/routing/routeKeys'
import type { WorkflowTask, WorkflowTaskKind } from '@/composables/useWorkflowTasks'
import type {
  CollaborationWorkItemQueue,
  CollaborationWorkItemSummary,
  CollaborationWorkItemTriggerType,
} from '@/types/collaboration'

const QUEUE_KIND: Record<CollaborationWorkItemQueue, WorkflowTaskKind> = {
  TEST: 'template-test',
  APPROVAL: 'template-approval',
  REMEDIATION: 'template-rework',
  PENDING_RELEASE: 'template-publish',
  ESCALATION: 'template-escalation',
}

const QUEUE_TITLE_KEY: Record<CollaborationWorkItemQueue, string> = {
  TEST: 'dashboard.tasks.templateTest.title',
  APPROVAL: 'dashboard.tasks.templateApproval.title',
  REMEDIATION: 'dashboard.tasks.templateRework.title',
  PENDING_RELEASE: 'dashboard.tasks.templatePublish.title',
  ESCALATION: 'collaboration.workItem.queue.ESCALATION.title',
}

const TRIGGER_DESCRIPTION_KEY: Record<CollaborationWorkItemTriggerType, string> = {
  SUBMIT_FOR_TEST: 'collaboration.workItem.trigger.SUBMIT_FOR_TEST.description',
  TEST_FAILURE_OR_RETURN_TO_DRAFT: 'collaboration.workItem.trigger.TEST_FAILURE_OR_RETURN_TO_DRAFT.description',
  SUBMIT_FOR_APPROVAL: 'collaboration.workItem.trigger.SUBMIT_FOR_APPROVAL.description',
  APPROVAL_FAILURE_OR_RETURN_TO_DRAFT: 'collaboration.workItem.trigger.APPROVAL_FAILURE_OR_RETURN_TO_DRAFT.description',
  APPROVAL_PENDING_RELEASE: 'collaboration.workItem.trigger.APPROVAL_PENDING_RELEASE.description',
  TIMEOUT_ESCALATION: 'collaboration.workItem.trigger.TIMEOUT_ESCALATION.description',
}

export type CollaborationWorkItemWithDisplay = CollaborationWorkItemSummary & {
  submitterDisplayName?: string | null
}

export function collaborationWorkItemToTask(item: CollaborationWorkItemWithDisplay): WorkflowTask {
  return {
    id: `collaboration-${item.workItemId}`,
    kind: QUEUE_KIND[item.queue],
    titleKey: QUEUE_TITLE_KEY[item.queue],
    descriptionKey: TRIGGER_DESCRIPTION_KEY[item.triggerType],
    path: templateLifecyclePanelPath(item.templateId),
    groupCode: item.groupCode,
    entityName: item.templateName,
    source: 'collaboration',
    workItemId: item.workItemId,
    templateId: item.templateId,
    queue: item.queue,
    triggerType: item.triggerType,
    submitterUserId: item.submitterUserId,
    submitterDisplayName: item.submitterDisplayName ?? undefined,
    summaryText: item.summaryText,
    ageSeconds: item.ageSeconds,
    createdAt: item.createdAt,
  }
}

export function formatCollaborationAgeSeconds(seconds: number): string {
  if (seconds < 60) {
    return '0m'
  }
  if (seconds < 3600) {
    return `${Math.floor(seconds / 60)}m`
  }
  if (seconds < 86400) {
    return `${Math.floor(seconds / 3600)}h`
  }
  return `${Math.floor(seconds / 86400)}d`
}
