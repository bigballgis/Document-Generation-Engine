import { computed, onMounted, watch, type ComputedRef } from 'vue'
import { useRoute } from 'vue-router'
import { parseDashboardTaskScope } from '@/composables/useWorkflowTasks'
import { canViewCollaborationWorkItems } from '@/auth/roles'
import { useCapabilities } from '@/composables/useCapabilities'
import type { ClusterOneRole } from '@/constants/roleJourneyDefinitions'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import {
  createDashboardCollaborationLoader,
  scrollToTasksIfRequested,
} from '@/composables/createDashboardCollaborationLoader'
import { ref } from 'vue'

export interface UseDashboardDataLoaderOptions {
  isOverviewTab: ComputedRef<boolean>
  isWorkflowTab: ComputedRef<boolean>
  isTaskTab: ComputedRef<boolean>
  primaryClusterOneRole: ComputedRef<ClusterOneRole | null>
  showTimeoutConfig: ComputedRef<boolean>
}

export function useDashboardDataLoader(options: UseDashboardDataLoaderOptions) {
  const { isOverviewTab, isWorkflowTab, isTaskTab, primaryClusterOneRole, showTimeoutConfig } =
    options

  const route = useRoute()
  const sessionStore = useSessionStore()
  const mastersStore = useMastersStore()
  const templatesStore = useTemplatesStore()
  const apiPolicyStore = useApiPolicyStore()
  const collaborationStore = useCollaborationStore()
  const { context, reviewMasters, manageMasters } = useCapabilities()

  const loading = ref(false)
  const mastersLoadError = ref(false)
  const templatesLoadError = ref(false)

  const canViewWorkItems = computed(() => canViewCollaborationWorkItems(context.value))

  const taskScope = computed(() =>
    parseDashboardTaskScope(route.query, {
      reviewMasters: reviewMasters.value,
      manageMasters: manageMasters.value,
    }),
  )

  const shouldLoadCollaborationWorkItems = computed(() => {
    if (!canViewWorkItems.value) {
      return false
    }
    if (isOverviewTab.value) {
      return true
    }
    if (isWorkflowTab.value) {
      return true
    }
    return isTaskTab.value && taskScope.value.fetchCollaboration
  })

  const showStatsSection = computed(() => !mastersLoadError.value && !templatesLoadError.value)

  const collaborationFetchFailed = computed(
    () =>
      taskScope.value.fetchCollaboration &&
      Boolean(collaborationStore.workItemsErrorMessageKey),
  )

  const collaborationLoadErrorKey = computed(
    () => collaborationStore.workItemsErrorMessageKey ?? 'collaboration.workItems.error.load',
  )

  const showCollaborationLoading = computed(
    () => taskScope.value.fetchCollaboration && collaborationStore.loadingWorkItems,
  )

  const { globalTimeoutConfig, groupTimeoutConfigs, fetchCollaborationWorkItems } =
    createDashboardCollaborationLoader({
      shouldLoadCollaborationWorkItems,
      taskScope,
      showTimeoutConfig,
      sessionStore,
      collaborationStore,
    })

  async function loadDashboardData() {
    loading.value = true
    mastersLoadError.value = false
    templatesLoadError.value = false

    const jobs: Promise<unknown>[] = []
    if (sessionStore.canAccessRoute('route.master-management')) {
      jobs.push(
        mastersStore.fetchAllMasters().catch(() => {
          mastersLoadError.value = true
        }),
      )
    }
    if (sessionStore.canAccessRoute('route.template-management')) {
      jobs.push(
        templatesStore.fetchAllTemplates().catch(() => {
          templatesLoadError.value = true
        }),
      )
    }
    if (shouldLoadCollaborationWorkItems.value) {
      jobs.push(
        fetchCollaborationWorkItems().catch(() => {
          /* error captured in collaboration store */
        }),
      )
    }
    if (sessionStore.canAccessRoute('route.api-policy-management')) {
      jobs.push(
        apiPolicyStore.fetchAlerts().catch(() => {
          /* degrade to zero-count stat card */
        }),
      )
    }

    await Promise.all(jobs)
    if (
      primaryClusterOneRole.value === 'MASTER_DESIGNER' &&
      sessionStore.canAccessRoute('route.master-management') &&
      !mastersLoadError.value
    ) {
      await mastersStore.enrichDraftMasterReviewHistory().catch(() => {
        /* degrade to summary-only journey mapping */
      })
    }
    loading.value = false
  }

  async function retryCollaborationLoad() {
    try {
      await fetchCollaborationWorkItems()
    } catch {
      /* error captured in collaboration store */
    }
  }

  onMounted(() => {
    void loadDashboardData()
    void scrollToTasksIfRequested(route.hash)
  })

  watch(
    () => [route.hash, route.query.queue, route.query.filter, route.query.tab] as const,
    () => {
      void scrollToTasksIfRequested(route.hash)
    },
  )

  watch(
    () => [route.query.queue, route.query.filter, route.query.tab] as const,
    () => {
      void fetchCollaborationWorkItems().catch(() => {
        /* error captured in collaboration store */
      })
    },
  )

  return {
    loading,
    mastersLoadError,
    templatesLoadError,
    globalTimeoutConfig,
    groupTimeoutConfigs,
    taskScope,
    shouldLoadCollaborationWorkItems,
    showStatsSection,
    collaborationFetchFailed,
    collaborationLoadErrorKey,
    showCollaborationLoading,
    loadDashboardData,
    retryCollaborationLoad,
    fetchCollaborationWorkItems,
  }
}
