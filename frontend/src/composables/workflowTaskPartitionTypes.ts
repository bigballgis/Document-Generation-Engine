import {
  canViewEscalationQueue,
  canAuthorTemplates,
  canDecideApprovals,
  canDecideTests,
  canPublishTemplates,
  MANAGEMENT_ROLES,
  type CapabilityContext,
} from '@/auth/roles'
import type {
  CollaborationWorkItemQueue,
  CollaborationWorkItemTriggerType,
} from '@/types/collaboration'

export type WorkflowTaskKind =
  | 'master-review'
  | 'master-rework'
  | 'template-test'
  | 'template-approval'
  | 'template-publish'
  | 'template-rework'
  | 'template-escalation'
  | 'clause-outdated-bump'
  | 'template-annual-review'
  | 'content-module-review'
  | 'content-module-rework'

type WorkflowTaskSource = 'master' | 'collaboration' | 'template' | 'content-module'

export interface WorkflowTask {
  id: string
  kind: WorkflowTaskKind
  titleKey: string
  descriptionKey: string
  path: string
  groupCode?: string
  entityName: string
  source?: WorkflowTaskSource
  workItemId?: string
  templateId?: string
  queue?: CollaborationWorkItemQueue
  triggerType?: CollaborationWorkItemTriggerType
  submitterUserId?: string
  submitterDisplayName?: string
  summaryText?: string
  ageSeconds?: number
  createdAt?: string
}

const COLLABORATION_QUEUES: readonly CollaborationWorkItemQueue[] = [
  'TEST',
  'APPROVAL',
  'REMEDIATION',
  'PENDING_RELEASE',
  'ESCALATION',
]

type DashboardTaskHubMode = 'unfiltered' | 'queue' | 'master-review'

export interface DashboardTaskScope {
  pageTitleKey: string
  pageDescriptionKey: string
  mode: DashboardTaskHubMode
  queueFilter: CollaborationWorkItemQueue | null
  fetchCollaboration: boolean
  showMasterReview: boolean
  showMasterRework: boolean
}

type TaskPartitionKind =
  | 'collaboration'
  | 'master-review'
  | 'master-rework'
  | 'clause-outdated-bump'
  | 'template-annual-review'
  | 'content-module-review'
  | 'content-module-rework'

export interface TaskPartition {
  id: string
  headingKey: string
  kind: TaskPartitionKind
  queue?: CollaborationWorkItemQueue
  tasks: WorkflowTask[]
}

function canSeeBehaviorRemediation(context: CapabilityContext): boolean {
  const hasEligibleRole = context.roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
      ] as string[]
    ).includes(role),
  )
  if (!hasEligibleRole) {
    return false
  }
  return canAuthorTemplates(context)
}

export function isValidCollaborationQueue(value: string): value is CollaborationWorkItemQueue {
  return (COLLABORATION_QUEUES as readonly string[]).includes(value)
}

export function getVisibleCollaborationQueues(context: CapabilityContext): CollaborationWorkItemQueue[] {
  const queues: CollaborationWorkItemQueue[] = []
  if (canDecideTests(context)) {
    queues.push('TEST')
  }
  if (canDecideApprovals(context)) {
    queues.push('APPROVAL')
  }
  if (canSeeBehaviorRemediation(context)) {
    queues.push('REMEDIATION')
  }
  if (canPublishTemplates(context)) {
    queues.push('PENDING_RELEASE')
  }
  if (canViewEscalationQueue(context)) {
    queues.push('ESCALATION')
  }
  return queues
}
