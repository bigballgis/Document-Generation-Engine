import type { CapabilityContext } from '@/auth/roles'
import {
  getVisibleCollaborationQueues,
  isValidCollaborationQueue,
  type DashboardTaskScope,
  type TaskPartition,
  type WorkflowTask,
} from '@/composables/workflowTaskPartitionTypes'

export type {
  WorkflowTask,
  WorkflowTaskKind,
  DashboardTaskScope,
  TaskPartition,
} from '@/composables/workflowTaskPartitionTypes'

export { getVisibleCollaborationQueues } from '@/composables/workflowTaskPartitionTypes'

export function sortTasksNewestFirst(items: WorkflowTask[]): WorkflowTask[] {
  return [...items].sort((left, right) => {
    const leftTime = left.createdAt ? Date.parse(left.createdAt) : 0
    const rightTime = right.createdAt ? Date.parse(right.createdAt) : 0
    return rightTime - leftTime
  })
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
