import { computed } from 'vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { canViewCollaborationWorkItems } from '@/auth/roles'
import { pathForRouteKey, ROUTE_KEYS } from '@/routing/routeKeys'
import { useAuthorWorkflowStore } from '@/stores/authorWorkflow'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { collaborationWorkItemToTask } from '@/utils/collaborationWorkItems'
import { isMasterReworkState } from '@/utils/masterDesignerJourney'
import type { MasterDocumentSummary, MasterReviewRecord } from '@/types/master'
import type { OutdatedClauseReferenceAuthorTask } from '@/api/authorWorkflow'
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
  const authorWorkflowStore = useAuthorWorkflowStore()
  const { manageMasters, reviewMasters, authorTemplates, context } = useCapabilities()

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

    if (authorTemplates.value) {
      for (const task of authorWorkflowStore.outdatedClauseTasks) {
        items.push(clauseOutdatedBumpTask(task))
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

function clauseOutdatedBumpTask(task: OutdatedClauseReferenceAuthorTask): WorkflowTask {
  return {
    id: `clause-outdated-${task.templateId}`,
    kind: 'clause-outdated-bump',
    titleKey: 'dashboard.tasks.clauseOutdatedBump.itemTitle',
    descriptionKey: 'dashboard.tasks.clauseOutdatedBump.description',
    path: `/templates/${task.templateId}/dev/${task.inFlightDevVersionId}?workspaceTab=design&designTab=contentModules`,
    groupCode: task.groupCode,
    entityName: task.name,
    source: 'template',
    templateId: task.templateId,
    summaryText: String(task.outdatedReferenceCount),
    createdAt: task.updatedAt,
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
