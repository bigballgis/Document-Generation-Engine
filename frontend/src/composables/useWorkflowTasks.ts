import { computed } from 'vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { canViewCollaborationWorkItems } from '@/auth/roles'
import { pathForRouteKey, ROUTE_KEYS } from '@/routing/routeKeys'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { collaborationWorkItemToTask } from '@/utils/collaborationWorkItems'
import { isMasterReworkState } from '@/utils/masterDesignerJourney'
import type { MasterDocumentSummary, MasterReviewRecord } from '@/types/master'
import {
  sortTasksNewestFirst,
  type WorkflowTask,
  type WorkflowTaskKind,
  type DashboardTaskScope,
  type TaskPartition,
  getVisibleCollaborationQueues,
  parseDashboardTaskScope,
  buildTaskPartitions,
} from '@/composables/workflowTaskPartitions'

export type {
  WorkflowTask,
  WorkflowTaskKind,
  DashboardTaskScope,
  TaskPartition,
}

export {
  getVisibleCollaborationQueues,
  parseDashboardTaskScope,
  buildTaskPartitions,
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
        if (!isMasterReworkCandidate(master, mastersStore.getDraftReviewHistory(master.id))) {
          continue
        }
        items.push(masterReworkTask(master))
      }
    }

    if (canViewCollaborationWorkItems(context.value)) {
      for (const workItem of collaborationStore.workItems) {
        items.push(collaborationWorkItemToTask(workItem))
      }
    }

    return sortTasksNewestFirst(items)
  })

  return { tasks }
}

function isMasterReworkCandidate(
  master: MasterDocumentSummary,
  reviewHistory?: MasterReviewRecord[],
): boolean {
  return isMasterReworkState(master.status, reviewHistory)
}

function masterReworkTask(master: MasterDocumentSummary): WorkflowTask {
  return {
    id: `master-rework-${master.id}`,
    kind: 'master-rework',
    titleKey: 'dashboard.tasks.masterRework.itemTitle',
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
