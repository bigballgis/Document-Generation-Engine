import { computed } from 'vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { templateDetailPath, pathForRouteKey, ROUTE_KEYS } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterDocumentSummary } from '@/types/master'
import type { TemplateSummary } from '@/types/template'

export type WorkflowTaskKind =
  | 'master-review'
  | 'template-test'
  | 'template-approval'
  | 'template-publish'
  | 'template-author-draft'

export interface WorkflowTask {
  id: string
  kind: WorkflowTaskKind
  titleKey: string
  descriptionKey: string
  path: string
  groupCode?: string
  entityName: string
}

export function useWorkflowTasks() {
  const mastersStore = useMastersStore()
  const templatesStore = useTemplatesStore()
  const {
    reviewMasters,
    decideTests,
    decideApprovals,
    publishTemplates,
    authorTemplates,
  } = useCapabilities()

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

    for (const template of templatesStore.templates) {
      if (decideTests.value && template.lifecycleStatus === 'TESTING') {
        items.push(templateTestTask(template))
      }
      if (decideApprovals.value && template.lifecycleStatus === 'APPROVAL') {
        items.push(templateApprovalTask(template))
      }
      if (publishTemplates.value && template.lifecycleStatus === 'PENDING_RELEASE') {
        items.push(templatePublishTask(template))
      }
      if (
        authorTemplates.value &&
        template.lifecycleStatus === 'DRAFT'
      ) {
        items.push(templateDraftTask(template))
      }
    }

    return items
  })

  return { tasks }
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
  }
}

function templateTestTask(template: TemplateSummary): WorkflowTask {
  return {
    id: `template-test-${template.id}`,
    kind: 'template-test',
    titleKey: 'dashboard.tasks.templateTest.title',
    descriptionKey: 'dashboard.tasks.templateTest.description',
    path: templateDetailPath(template.id, 'overview'),
    groupCode: template.groupCode,
    entityName: template.name,
  }
}

function templateApprovalTask(template: TemplateSummary): WorkflowTask {
  return {
    id: `template-approval-${template.id}`,
    kind: 'template-approval',
    titleKey: 'dashboard.tasks.templateApproval.title',
    descriptionKey: 'dashboard.tasks.templateApproval.description',
    path: templateDetailPath(template.id, 'overview'),
    groupCode: template.groupCode,
    entityName: template.name,
  }
}

function templatePublishTask(template: TemplateSummary): WorkflowTask {
  return {
    id: `template-publish-${template.id}`,
    kind: 'template-publish',
    titleKey: 'dashboard.tasks.templatePublish.title',
    descriptionKey: 'dashboard.tasks.templatePublish.description',
    path: templateDetailPath(template.id, 'overview'),
    groupCode: template.groupCode,
    entityName: template.name,
  }
}

function templateDraftTask(template: TemplateSummary): WorkflowTask {
  return {
    id: `template-draft-${template.id}`,
    kind: 'template-author-draft',
    titleKey: 'dashboard.tasks.templateDraft.title',
    descriptionKey: 'dashboard.tasks.templateDraft.description',
    path: templateDetailPath(template.id, 'overview'),
    groupCode: template.groupCode,
    entityName: template.name,
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
