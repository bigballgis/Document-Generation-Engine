import { computed } from 'vue'
import { useCapabilities } from '@/composables/useCapabilities'
import {
  canViewEscalationQueue,
  canAuthorTemplates,
  canDecideApprovals,
  canDecideTests,
  canPublishTemplates,
  canViewCollaborationWorkItems,
} from '@/auth/roles'
import { pathForRouteKey, ROUTE_KEYS } from '@/routing/routeKeys'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { collaborationWorkItemToTask } from '@/utils/collaborationWorkItems'
import type {
  CollaborationWorkItemQueue,
  CollaborationWorkItemTriggerType,
} from '@/types/collaboration'
import type { CapabilityContext } from '@/auth/roles'
import type { MasterDocumentSummary } from '@/types/master'

export type WorkflowTaskKind =
  | 'master-review'
  | 'master-rework'
  | 'template-test'
  | 'template-approval'
  | 'template-publish'
  | 'template-rework'
  | 'template-escalation'

export type WorkflowTaskSource = 'master' | 'collaboration'

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
  summaryText?: string
  ageSeconds?: number
  createdAt?: string
}

export const COLLABORATION_QUEUES: readonly CollaborationWorkItemQueue[] = [
  'TEST',
  'APPROVAL',
  'REMEDIATION',
  'PENDING_RELEASE',
  'ESCALATION',
]

export type DashboardTaskHubMode = 'unfiltered' | 'queue' | 'master-review'

export interface DashboardTaskScope {
  pageTitleKey: string
  pageDescriptionKey: string
  mode: DashboardTaskHubMode
  queueFilter: CollaborationWorkItemQueue | null
  fetchCollaboration: boolean
  showMasterReview: boolean
  showMasterRework: boolean
}

export type TaskPartitionKind = 'collaboration' | 'master-review' | 'master-rework'

export interface TaskPartition {
  id: string
  headingKey: string
  kind: TaskPartitionKind
  queue?: CollaborationWorkItemQueue
  tasks: WorkflowTask[]
}

function sortTasksNewestFirst(items: WorkflowTask[]): WorkflowTask[] {
  return [...items].sort((left, right) => {
    const leftTime = left.createdAt ? Date.parse(left.createdAt) : 0
    const rightTime = right.createdAt ? Date.parse(right.createdAt) : 0
    return rightTime - leftTime
  })
}

function canSeeBehaviorRemediation(context: CapabilityContext): boolean {
  const hasEligibleRole = context.roles.some((role) =>
    (['GLOBAL_ADMIN', 'GROUP_ADMIN', 'TEMPLATE_AUTHOR'] as string[]).includes(role),
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

export function useWorkflowTasks() {
  const mastersStore = useMastersStore()
  const collaborationStore = useCollaborationStore()
  const { manageMasters, reviewMasters, context } = useCapabilities()

  const tasks = computed<WorkflowTask[]>(() => {
    const items: WorkflowTask[] = []

    if (reviewMasters.value) {
      for (const master of mastersStore.masters) {
        if (master.status !== 'PENDING_REVIEW') {
          continue
        }
        items.push(masterReviewTask(master))
      }
    }

    if (manageMasters.value) {
      for (const master of mastersStore.masters) {
        if (master.status !== 'REJECTED') {
          continue
        }
        items.push(masterReworkTask(master))
      }
    }

    if (canViewCollaborationWorkItems(context.value.roles)) {
      for (const workItem of collaborationStore.workItems) {
        items.push(collaborationWorkItemToTask(workItem))
      }
    }

    return sortTasksNewestFirst(items)
  })

  return { tasks }
}

function masterReworkTask(master: MasterDocumentSummary): WorkflowTask {
  return {
    id: `master-rework-${master.id}`,
    kind: 'master-rework',
    titleKey: 'dashboard.tasks.masterRework.title',
    descriptionKey: 'dashboard.tasks.masterRework.description',
    path: `/masters/${master.id}`,
    groupCode: master.groupCode,
    entityName: master.name,
    source: 'master',
    createdAt: master.updatedAt,
  }
}

function masterReviewTask(master: MasterDocumentSummary): WorkflowTask {
  return {
    id: `master-review-${master.id}`,
    kind: 'master-review',
    titleKey: 'dashboard.tasks.masterReview.title',
    descriptionKey: 'dashboard.tasks.masterReview.description',
    path: `/masters/${master.id}`,
    groupCode: master.groupCode,
    entityName: master.name,
    source: 'master',
    createdAt: master.updatedAt,
  }
}

export function dashboardQuickLinks(visibleRoutes: string[]) {
  const allowed = new Set(visibleRoutes)
  const links: Array<{ labelKey: string; path: string }> = []

  if (allowed.has(ROUTE_KEYS.templateManagement)) {
    links.push({
      labelKey: 'dashboard.quickLinks.templates',
      path: pathForRouteKey(ROUTE_KEYS.templateManagement),
    })
  }
  if (allowed.has(ROUTE_KEYS.masterManagement)) {
    links.push({
      labelKey: 'dashboard.quickLinks.masters',
      path: pathForRouteKey(ROUTE_KEYS.masterManagement),
    })
  }
  if (allowed.has(ROUTE_KEYS.apiPolicyManagement)) {
    links.push({
      labelKey: 'dashboard.quickLinks.apiPolicies',
      path: pathForRouteKey(ROUTE_KEYS.apiPolicyManagement),
    })
  }
  return links
}
