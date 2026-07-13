import { nextTick, reactive, ref, type ComputedRef, type Ref } from 'vue'
import { MANAGEMENT_ROLES } from '@/auth/roles'
import * as collaborationApi from '@/api/collaboration'
import type { CollaborationTimeoutConfig, CollaborationWorkItemQueue } from '@/types/collaboration'
import type { DashboardTaskScope } from '@/composables/workflowTaskPartitions'

type CollaborationStoreLike = {
  workItems: { groupCode?: string | null }[]
  fetchWorkItems: (params?: { queue: CollaborationWorkItemQueue }) => Promise<void>
}

type SessionStoreLike = {
  hasRole: (role: string) => boolean
}

export function createDashboardCollaborationLoader(deps: {
  shouldLoadCollaborationWorkItems: ComputedRef<boolean>
  taskScope: ComputedRef<DashboardTaskScope>
  showTimeoutConfig: ComputedRef<boolean>
  sessionStore: SessionStoreLike
  collaborationStore: CollaborationStoreLike
}) {
  const globalTimeoutConfig = ref<CollaborationTimeoutConfig | null>(null)
  const groupTimeoutConfigs = reactive<Record<string, CollaborationTimeoutConfig | null>>({})

  function resolveCollaborationFetchParams():
    | { queue: CollaborationWorkItemQueue }
    | undefined {
    if (!deps.taskScope.value.fetchCollaboration) {
      return undefined
    }
    if (deps.taskScope.value.queueFilter) {
      return { queue: deps.taskScope.value.queueFilter }
    }
    return undefined
  }

  async function loadTimeoutConfigsForWorkItems() {
    if (!deps.shouldLoadCollaborationWorkItems.value) {
      globalTimeoutConfig.value = null
      return
    }

    if (deps.sessionStore.hasRole(MANAGEMENT_ROLES.GLOBAL_ADMIN)) {
      try {
        globalTimeoutConfig.value = await collaborationApi.getCollaborationTimeoutConfig()
      } catch {
        globalTimeoutConfig.value = null
      }
    } else {
      globalTimeoutConfig.value = null
    }

    if (!deps.showTimeoutConfig.value) {
      return
    }

    const groupCodes = [
      ...new Set(
        deps.collaborationStore.workItems
          .map((item) => item.groupCode)
          .filter((code): code is string => Boolean(code)),
      ),
    ]

    await Promise.all(
      groupCodes.map(async (groupCode) => {
        if (groupCode in groupTimeoutConfigs) {
          return
        }
        try {
          groupTimeoutConfigs[groupCode] = await collaborationApi.getCollaborationTimeoutConfig(groupCode)
        } catch {
          groupTimeoutConfigs[groupCode] = null
        }
      }),
    )
  }

  async function fetchCollaborationWorkItems() {
    if (!deps.shouldLoadCollaborationWorkItems.value) {
      return
    }
    const params = resolveCollaborationFetchParams()
    await deps.collaborationStore.fetchWorkItems(params)
    await loadTimeoutConfigsForWorkItems()
  }

  return {
    globalTimeoutConfig: globalTimeoutConfig as Ref<CollaborationTimeoutConfig | null>,
    groupTimeoutConfigs,
    fetchCollaborationWorkItems,
  }
}

export async function scrollToTasksIfRequested(hash: string) {
  if (hash !== '#tasks-section') {
    return
  }
  await nextTick()
  document.getElementById('tasks-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
