import { computed, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getVisibleCollaborationQueues } from '@/composables/useWorkflowTasks'
import { useCapabilities } from '@/composables/useCapabilities'
import type { CollaborationWorkItemQueue } from '@/types/collaboration'

export type DashboardTabKey = 'overview' | 'workflow' | CollaborationWorkItemQueue | 'master-review'

export interface DashboardTab {
  key: DashboardTabKey
  labelKey: string
  useJourneyTitle?: boolean
}

export interface UseDashboardTabsOptions {
  showJourneySection: ComputedRef<boolean>
  journeyTitleKey: ComputedRef<string | undefined>
}

function queueToNavKey(queue: CollaborationWorkItemQueue): string {
  const map: Record<CollaborationWorkItemQueue, string> = {
    TEST: 'testing',
    APPROVAL: 'approval',
    REMEDIATION: 'remediation',
    PENDING_RELEASE: 'pendingRelease',
    ESCALATION: 'escalation',
  }
  return map[queue]
}

export function useDashboardTabs(options: UseDashboardTabsOptions) {
  const { showJourneySection, journeyTitleKey } = options

  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const { context, reviewMasters, manageMasters } = useCapabilities()

  const visibleTabs = computed((): DashboardTab[] => {
    const tabs: DashboardTab[] = [{ key: 'overview', labelKey: 'dashboard.tabs.overview' }]
    for (const queue of getVisibleCollaborationQueues(context.value)) {
      const labelKey = `nav.behaviorItems.${queueToNavKey(queue)}`
      tabs.push({ key: queue, labelKey })
    }
    if (reviewMasters.value || manageMasters.value) {
      tabs.push({ key: 'master-review', labelKey: 'nav.behaviorItems.masterReview' })
    }
    if (showJourneySection.value) {
      tabs.push({
        key: 'workflow',
        labelKey: 'dashboard.tabs.workflow',
        useJourneyTitle: true,
      })
    }
    return tabs
  })

  const activeTab = computed((): DashboardTabKey => {
    const tabQuery = typeof route.query.tab === 'string' ? route.query.tab : undefined
    if (tabQuery === 'workflow' && showJourneySection.value) {
      return 'workflow'
    }
    const filter = typeof route.query.filter === 'string' ? route.query.filter : undefined
    if (filter === 'master-review') {
      return 'master-review'
    }
    const q = typeof route.query.queue === 'string' ? route.query.queue : undefined
    if (q && visibleTabs.value.some((tab) => tab.key === q)) {
      return q as DashboardTabKey
    }
    return 'overview'
  })

  /** Authors often have only Overview; `#tasks-section` still opens the task list (CE-U07). */
  const forceTasksFromHash = computed(() => route.hash === '#tasks-section')

  const isOverviewTab = computed(
    () => activeTab.value === 'overview' && !forceTasksFromHash.value,
  )
  const isWorkflowTab = computed(() => activeTab.value === 'workflow')
  const isTaskTab = computed(
    () => (!isOverviewTab.value && !isWorkflowTab.value) || forceTasksFromHash.value,
  )

  function tabLabel(tab: DashboardTab): string {
    if (tab.useJourneyTitle && journeyTitleKey.value) {
      return t(journeyTitleKey.value)
    }
    return t(tab.labelKey)
  }

  function handleTabChange(tabKey: string) {
    if (tabKey === 'overview') {
      void router.replace({ path: '/dashboard' })
    } else if (tabKey === 'workflow') {
      void router.replace({ path: '/dashboard', query: { tab: 'workflow' } })
    } else if (tabKey === 'master-review') {
      void router.replace({ path: '/dashboard', query: { filter: 'master-review' } })
    } else {
      void router.replace({ path: '/dashboard', query: { queue: tabKey } })
    }
  }

  return {
    visibleTabs,
    activeTab,
    isOverviewTab,
    isWorkflowTab,
    isTaskTab,
    tabLabel,
    handleTabChange,
  }
}
