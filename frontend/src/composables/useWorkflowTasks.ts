import { computed } from 'vue'
import { useCapabilities } from '@/composables/useCapabilities'
import {
  canAccessApproverWorkbench,
  canAccessCollaborationEscalationWorkbench,
  canAccessTesterWorkbench,
  canViewCollaborationWorkItems,
  type CapabilityContext,
} from '@/auth/roles'
import { pathForRouteKey, ROUTE_KEYS } from '@/routing/routeKeys'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { collaborationWorkItemToTask } from '@/utils/collaborationWorkItems'
import type {
  CollaborationWorkItemQueue,
  CollaborationWorkItemTriggerType,
} from '@/types/collaboration'
import type { MasterDocumentSummary } from '@/types/master'

export type WorkflowTaskKind =
  | 'master-review'
  | 'master-rework'
  | 'template-test'
  | 'template-approval'
  | 'template-publish'
  | 'template-author-draft'
  | 'template-rework'

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

    return items
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
  }
}

export function dashboardQuickLinks(visibleRoutes: string[], context?: CapabilityContext) {
  const allowed = new Set(visibleRoutes)
  const links: Array<{ labelKey: string; path: string }> = []

  if (context && canAccessTesterWorkbench(context)) {
    links.push({
      labelKey: 'dashboard.quickLinks.testerWorkbench',
      path: pathForRouteKey(ROUTE_KEYS.testerWorkbench),
    })
  }
  if (context && canAccessApproverWorkbench(context)) {
    links.push({
      labelKey: 'dashboard.quickLinks.approverWorkbench',
      path: pathForRouteKey(ROUTE_KEYS.approverWorkbench),
    })
  }
  if (context && canAccessCollaborationEscalationWorkbench(context)) {
    links.push({
      labelKey: 'dashboard.quickLinks.escalationWorkbench',
      path: pathForRouteKey(ROUTE_KEYS.escalationWorkbench),
    })
  }

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
