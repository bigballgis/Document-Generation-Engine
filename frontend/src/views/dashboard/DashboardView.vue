<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import DashboardOverviewTab from '@/components/dashboard/DashboardOverviewTab.vue'
import DashboardWorkflowTab from '@/components/dashboard/DashboardWorkflowTab.vue'
import DashboardTasksTab from '@/components/dashboard/DashboardTasksTab.vue'
import { useDashboardStats } from '@/composables/useDashboardStats'
import { useDashboardJourney } from '@/composables/useDashboardJourney'
import { useDashboardTabs } from '@/composables/useDashboardTabs'
import { useDashboardDataLoader } from '@/composables/useDashboardDataLoader'
import { buildTaskPartitions, useWorkflowTasks } from '@/composables/useWorkflowTasks'
import { useCapabilities } from '@/composables/useCapabilities'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()
const { context } = useCapabilities()
const { tasks } = useWorkflowTasks()

const {
  showJourneySection,
  showTimeoutConfig,
  journeySteps,
  journeyTitleKey,
  journeyCurrentStepIndex,
  journeyGuidanceKey,
  dashboardJourneyPath,
  openDashboardJourney,
  primaryClusterOneRole,
} = useDashboardJourney()

const {
  visibleTabs,
  activeTab,
  isOverviewTab,
  isWorkflowTab,
  isTaskTab,
  tabLabel,
  handleTabChange,
} = useDashboardTabs({ showJourneySection, journeyTitleKey })

const {
  loading,
  mastersLoadError,
  templatesLoadError,
  globalTimeoutConfig,
  groupTimeoutConfigs,
  taskScope,
  showStatsSection,
  collaborationFetchFailed,
  collaborationLoadErrorKey,
  showCollaborationLoading,
  loadDashboardData,
  retryCollaborationLoad,
} = useDashboardDataLoader({
  isOverviewTab,
  isWorkflowTab,
  isTaskTab,
  primaryClusterOneRole,
  showTimeoutConfig,
})

const visibleRoutes = computed(() => sessionStore.session?.visibleRoutes ?? [])
const { stats } = useDashboardStats(visibleRoutes)

const authorizedGroupsSummary = computed(() => {
  const groups = sessionStore.session?.authorizedGroupCodes ?? []
  if (groups.includes('*')) {
    return t('home.summary.allGroups')
  }
  if (groups.length === 0) {
    return t('home.summary.noGroups')
  }
  return groups.join(', ')
})

const pageDescription = computed(() => {
  if (isOverviewTab.value) {
    return `${t('dashboard.description')} ${t('home.summary.authorizedGroups')}: ${authorizedGroupsSummary.value}`
  }
  if (isWorkflowTab.value) {
    return `${t('dashboard.workflowTab.description')} ${t('home.summary.authorizedGroups')}: ${authorizedGroupsSummary.value}`
  }
  const base = t(taskScope.value.pageDescriptionKey)
  return `${base} ${t('home.summary.authorizedGroups')}: ${authorizedGroupsSummary.value}`
})

const partitions = computed(() =>
  buildTaskPartitions(taskScope.value, tasks.value, context.value),
)

const visiblePartitions = computed(() => {
  if (collaborationFetchFailed.value) {
    return partitions.value.filter((partition) => partition.kind !== 'collaboration')
  }
  return partitions.value
})

function openTask(path: string) {
  router.push(path)
}
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('dashboard.title')"
      :description="pageDescription"
    />

    <LoadErrorPanel
      v-if="mastersLoadError"
      message-key="home.dashboard.loadError"
      @retry="loadDashboardData"
    />

    <LoadErrorPanel
      v-if="templatesLoadError"
      message-key="dashboard.loadError"
      @retry="loadDashboardData"
    />

    <el-tabs
      v-if="visibleTabs.length > 1"
      :model-value="activeTab"
      class="dashboard-tabs"
      @tab-change="handleTabChange"
    >
      <el-tab-pane
        v-for="tab in visibleTabs"
        :key="tab.key"
        :label="tabLabel(tab)"
        :name="tab.key"
      />
    </el-tabs>

    <DashboardOverviewTab
      v-if="isOverviewTab"
      :stats="stats"
      :loading="loading"
      :show-stats-section="showStatsSection"
    />

    <DashboardWorkflowTab
      v-else-if="isWorkflowTab"
      :journey-steps="journeySteps"
      :journey-current-step-index="journeyCurrentStepIndex"
      :journey-guidance-key="journeyGuidanceKey"
      :journey-title-key="journeyTitleKey"
      :dashboard-journey-path="dashboardJourneyPath"
      @open-journey="openDashboardJourney"
    />

    <DashboardTasksTab
      v-else
      :loading="loading"
      :show-collaboration-loading="showCollaborationLoading"
      :collaboration-fetch-failed="collaborationFetchFailed"
      :collaboration-load-error-key="collaborationLoadErrorKey"
      :visible-partitions="visiblePartitions"
      :global-timeout-config="globalTimeoutConfig"
      :group-timeout-configs="groupTimeoutConfigs"
      @retry-collaboration="retryCollaborationLoad"
      @open-task="openTask"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.dashboard-tabs {
  margin-bottom: var(--space-6);

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: var(--border-default);
  }

  :deep(.el-tabs__item) {
    font-size: var(--font-size-sm);
    font-weight: 500;
    color: var(--text-secondary);
    padding: 0 var(--space-4);
    height: 40px;
    line-height: 40px;

    &.is-active {
      color: var(--brand-primary);
      font-weight: 600;
    }

    &:hover {
      color: var(--brand-primary);
    }
  }

  :deep(.el-tabs__active-bar) {
    background: var(--brand-primary);
    height: 2px;
  }
}
</style>
