import { computed } from 'vue'
import { pathForRouteKey, ROUTE_KEYS } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'
import { useWorkflowTasks } from '@/composables/useWorkflowTasks'

export interface DashboardStatCard {
  key: string
  count: number
  titleKey: string
  descriptionKey: string
  actionKey: string
  path: string
}

export function useDashboardStats() {
  const mastersStore = useMastersStore()
  const templatesStore = useTemplatesStore()
  const { tasks } = useWorkflowTasks()

  const stats = computed<DashboardStatCard[]>(() => {
    const masters = mastersStore.masters
    const templates = templatesStore.templates

    const pendingMasterReviews = masters.filter((item) => item.status === 'PENDING_REVIEW').length
    const masterVersionsInProgress = masters.filter(
      (item) => item.status === 'DRAFT' || item.status === 'REJECTED',
    ).length
    const templateVersionsInWorkflow = templates.filter((item) =>
      ['DRAFT', 'TESTING', 'APPROVAL', 'PENDING_RELEASE'].includes(item.lifecycleStatus),
    ).length
    const publishedVersions = templates.filter((item) => item.lifecycleStatus === 'PUBLISHED').length
    const stoppedVersions = templates.filter((item) => item.lifecycleStatus === 'STOPPED').length

    return [
      {
        key: 'pendingActions',
        count: tasks.value.length,
        titleKey: 'dashboard.stats.pendingActions.title',
        descriptionKey: 'dashboard.stats.pendingActions.description',
        actionKey: 'dashboard.stats.pendingActions.action',
        path: '/dashboard',
      },
      {
        key: 'masterPendingReview',
        count: pendingMasterReviews,
        titleKey: 'dashboard.stats.masterPendingReview.title',
        descriptionKey: 'dashboard.stats.masterPendingReview.description',
        actionKey: 'dashboard.stats.masterPendingReview.action',
        path: pathForRouteKey(ROUTE_KEYS.masterManagement),
      },
      {
        key: 'masterVersionsInProgress',
        count: masterVersionsInProgress,
        titleKey: 'dashboard.stats.masterVersionsInProgress.title',
        descriptionKey: 'dashboard.stats.masterVersionsInProgress.description',
        actionKey: 'dashboard.stats.masterVersionsInProgress.action',
        path: pathForRouteKey(ROUTE_KEYS.masterManagement),
      },
      {
        key: 'templateVersionsInWorkflow',
        count: templateVersionsInWorkflow,
        titleKey: 'dashboard.stats.templateVersionsInWorkflow.title',
        descriptionKey: 'dashboard.stats.templateVersionsInWorkflow.description',
        actionKey: 'dashboard.stats.templateVersionsInWorkflow.action',
        path: pathForRouteKey(ROUTE_KEYS.templateManagement),
      },
      {
        key: 'publishedVersions',
        count: publishedVersions,
        titleKey: 'dashboard.stats.publishedVersions.title',
        descriptionKey: 'dashboard.stats.publishedVersions.description',
        actionKey: 'dashboard.stats.publishedVersions.action',
        path: pathForRouteKey(ROUTE_KEYS.templateManagement),
      },
      {
        key: 'stoppedVersions',
        count: stoppedVersions,
        titleKey: 'dashboard.stats.stoppedVersions.title',
        descriptionKey: 'dashboard.stats.stoppedVersions.description',
        actionKey: 'dashboard.stats.stoppedVersions.action',
        path: pathForRouteKey(ROUTE_KEYS.templateManagement),
      },
      {
        key: 'catalogMasters',
        count: masters.length,
        titleKey: 'dashboard.stats.catalogMasters.title',
        descriptionKey: 'dashboard.stats.catalogMasters.description',
        actionKey: 'dashboard.stats.catalogMasters.action',
        path: pathForRouteKey(ROUTE_KEYS.masterManagement),
      },
      {
        key: 'catalogTemplates',
        count: templates.length,
        titleKey: 'dashboard.stats.catalogTemplates.title',
        descriptionKey: 'dashboard.stats.catalogTemplates.description',
        actionKey: 'dashboard.stats.catalogTemplates.action',
        path: pathForRouteKey(ROUTE_KEYS.templateManagement),
      },
    ]
  })

  return { stats }
}
