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

type WorkflowTaskSource = 'master' | 'collaboration'

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

type TaskPartitionKind = 'collaboration' | 'master-review' | 'master-rework'

export interface TaskPartition {
  id: string
  headingKey: string
  kind: TaskPartitionKind
  queue?: CollaborationWorkItemQueue
  tasks: WorkflowTask[]
}

export function sortTasksNewestFirst(items: WorkflowTask[]): WorkflowTask[] {
  return [...items].sort((left, right) => {
    const leftTime = left.createdAt ? Date.parse(left.createdAt) : 0
    const rightTime = right.createdAt ? Date.parse(right.createdAt) : 0
    return rightTime - leftTime
  })
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

function isValidCollaborationQueue(value: string): value is CollaborationWorkItemQueue {
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

export function parseDashboardTaskScope(
  query: Record<string, unknown>,
  options: { reviewMasters: boolean; manageMasters: boolean },
): DashboardTaskScope {
  const filter = typeof query.filter === 'string' ? query.filter : undefined
  if (filter === 'master-review') {
    return {
      pageTitleKey: 'nav.behaviorItems.masterReview',
      pageDescriptionKey: 'dashboard.tasks.description',
      mode: 'master-review',
      queueFilter: null,
      fetchCollaboration: false,
      showMasterReview: options.reviewMasters,
      showMasterRework: options.manageMasters,
    }
  }

  const rawQueue = typeof query.queue === 'string' ? query.queue : undefined
  if (rawQueue && isValidCollaborationQueue(rawQueue)) {
    return {
      pageTitleKey: `collaboration.workItem.queue.${rawQueue}.title`,
      pageDescriptionKey: 'dashboard.tasks.description',
      mode: 'queue',
      queueFilter: rawQueue,
      fetchCollaboration: true,
      showMasterReview: false,
      showMasterRework: false,
    }
  }

  return {
    pageTitleKey: 'dashboard.title',
    pageDescriptionKey: 'dashboard.description',
    mode: 'unfiltered',
    queueFilter: null,
    fetchCollaboration: true,
    showMasterReview: options.reviewMasters,
    showMasterRework: options.manageMasters,
  }
}

export function buildTaskPartitions(
  scope: DashboardTaskScope,
  tasks: WorkflowTask[],
  context: CapabilityContext,
): TaskPartition[] {
  const partitions: TaskPartition[] = []

  if (scope.mode === 'master-review') {
    if (scope.showMasterReview) {
      partitions.push({
        id: 'master-review',
        headingKey: 'dashboard.tasks.masterReview.title',
        kind: 'master-review',
        tasks: sortTasksNewestFirst(tasks.filter((task) => task.kind === 'master-review')),
      })
    }
    if (scope.showMasterRework) {
      partitions.push({
        id: 'master-rework',
        headingKey: 'dashboard.tasks.masterRework.title',
        kind: 'master-rework',
        tasks: sortTasksNewestFirst(tasks.filter((task) => task.kind === 'master-rework')),
      })
    }
    return partitions
  }

  if (scope.mode === 'queue' && scope.queueFilter) {
    partitions.push({
      id: `queue-${scope.queueFilter}`,
      headingKey: `collaboration.workItem.queue.${scope.queueFilter}.label`,
      kind: 'collaboration',
      queue: scope.queueFilter,
      tasks: sortTasksNewestFirst(
        tasks.filter(
          (task) => task.source === 'collaboration' && task.queue === scope.queueFilter,
        ),
      ),
    })
    return partitions
  }

  for (const queue of getVisibleCollaborationQueues(context)) {
    partitions.push({
      id: `queue-${queue}`,
      headingKey: `collaboration.workItem.queue.${queue}.label`,
      kind: 'collaboration',
      queue,
      tasks: sortTasksNewestFirst(
        tasks.filter((task) => task.source === 'collaboration' && task.queue === queue),
      ),
    })
  }

  if (scope.showMasterReview) {
    partitions.push({
      id: 'master-review',
      headingKey: 'dashboard.tasks.masterReview.title',
      kind: 'master-review',
      tasks: sortTasksNewestFirst(tasks.filter((task) => task.kind === 'master-review')),
    })
  }

  if (scope.showMasterRework) {
    partitions.push({
      id: 'master-rework',
      headingKey: 'dashboard.tasks.masterRework.title',
      kind: 'master-rework',
      tasks: sortTasksNewestFirst(tasks.filter((task) => task.kind === 'master-rework')),
    })
  }

  return partitions
}
