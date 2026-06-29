<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import DashboardStatCards from '@/components/dashboard/DashboardStatCards.vue'
import TaskHubPartitionSection from '@/components/dashboard/TaskHubPartitionSection.vue'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import {
  resolveClusterOneJourney,
  resolvePrimaryClusterOneRole,
  roleJourneyTitleKey,
} from '@/constants/roleJourneyDefinitions'
import {
  resolveMasterDesignerDashboardJourneyIndex,
  type MasterDesignerDashboardMaster,
} from '@/utils/masterDesignerJourney'
import { useDashboardStats } from '@/composables/useDashboardStats'
import {
  buildTaskPartitions,
  dashboardQuickLinks,
  parseDashboardTaskScope,
  useWorkflowTasks,
} from '@/composables/useWorkflowTasks'
import { canMaintainCollaborationTimeoutConfig, canViewCollaborationWorkItems } from '@/auth/roles'
import { useCapabilities } from '@/composables/useCapabilities'
import CollaborationTimeoutConfigPanel from '@/components/collaboration/CollaborationTimeoutConfigPanel.vue'
import * as collaborationApi from '@/api/collaboration'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { CollaborationTimeoutConfig, CollaborationWorkItemQueue } from '@/types/collaboration'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const mastersStore = useMastersStore()
const templatesStore = useTemplatesStore()
const collaborationStore = useCollaborationStore()
const { context, reviewMasters, manageMasters } = useCapabilities()
const { tasks } = useWorkflowTasks()
const visibleRoutes = computed(() => sessionStore.session?.visibleRoutes ?? [])
const { stats } = useDashboardStats(visibleRoutes)

const loading = ref(false)
const mastersLoadError = ref(false)
const templatesLoadError = ref(false)

const globalTimeoutConfig = ref<CollaborationTimeoutConfig | null>(null)
const groupTimeoutConfigs = reactive<Record<string, CollaborationTimeoutConfig | null>>({})

const quickLinks = computed(() =>
  dashboardQuickLinks(sessionStore.session?.visibleRoutes ?? []),
)

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

const canViewWorkItems = computed(() =>
  canViewCollaborationWorkItems(sessionStore.session?.roles ?? []),
)

const showTimeoutConfig = computed(() => canMaintainCollaborationTimeoutConfig(context.value))

const taskScope = computed(() =>
  parseDashboardTaskScope(route.query, {
    reviewMasters: reviewMasters.value,
    manageMasters: manageMasters.value,
  }),
)

const partitions = computed(() =>
  buildTaskPartitions(taskScope.value, tasks.value, context.value),
)

const collaborationFetchFailed = computed(
  () =>
    taskScope.value.fetchCollaboration &&
    Boolean(collaborationStore.workItemsErrorMessageKey),
)

const visiblePartitions = computed(() => {
  if (collaborationFetchFailed.value) {
    return partitions.value.filter((partition) => partition.kind !== 'collaboration')
  }
  return partitions.value
})

const showStatsSection = computed(() => !mastersLoadError.value && !templatesLoadError.value)

const primaryClusterOneRole = computed(() =>
  resolvePrimaryClusterOneRole(sessionStore.session?.roles ?? []),
)

const journeySteps = computed(() =>
  primaryClusterOneRole.value
    ? resolveClusterOneJourney(primaryClusterOneRole.value)
    : [],
)

const journeyTitleKey = computed(() =>
  primaryClusterOneRole.value
    ? roleJourneyTitleKey(primaryClusterOneRole.value)
    : undefined,
)

const masterDesignerJourneyResolution = computed(() => {
  if (primaryClusterOneRole.value !== 'MASTER_DESIGNER') {
    return null
  }
  const enrichedMasters: MasterDesignerDashboardMaster[] = mastersStore.masters.map((master) => ({
    ...master,
    reviewHistory: mastersStore.getDraftReviewHistory(master.id),
  }))
  return resolveMasterDesignerDashboardJourneyIndex(enrichedMasters)
})

const journeyCurrentStepIndex = computed(() => {
  if (primaryClusterOneRole.value === 'MASTER_DESIGNER') {
    return masterDesignerJourneyResolution.value?.currentStepIndex ?? null
  }
  return null
})

const journeyGuidanceKey = computed(() => {
  if (primaryClusterOneRole.value === 'MASTER_DESIGNER') {
    return masterDesignerJourneyResolution.value?.guidanceKey
  }
  return undefined
})

const collaborationLoadErrorKey = computed(
  () => collaborationStore.workItemsErrorMessageKey ?? 'collaboration.workItems.error.load',
)

const showCollaborationLoading = computed(
  () => taskScope.value.fetchCollaboration && collaborationStore.loadingWorkItems,
)

function resolveCollaborationFetchParams():
  | { queue: CollaborationWorkItemQueue }
  | undefined {
  if (!taskScope.value.fetchCollaboration) {
    return undefined
  }
  if (taskScope.value.queueFilter) {
    return { queue: taskScope.value.queueFilter }
  }
  return undefined
}

async function loadTimeoutConfigsForWorkItems() {
  if (!canViewWorkItems.value || !taskScope.value.fetchCollaboration) {
    globalTimeoutConfig.value = null
    return
  }

  if (sessionStore.hasRole('GLOBAL_ADMIN')) {
    try {
      globalTimeoutConfig.value = await collaborationApi.getCollaborationTimeoutConfig()
    } catch {
      globalTimeoutConfig.value = null
    }
  } else {
    globalTimeoutConfig.value = null
  }

  if (!showTimeoutConfig.value) {
    return
  }

  const groupCodes = [
    ...new Set(
      collaborationStore.workItems
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
  if (!canViewWorkItems.value || !taskScope.value.fetchCollaboration) {
    return
  }
  const params = resolveCollaborationFetchParams()
  await collaborationStore.fetchWorkItems(params)
  await loadTimeoutConfigsForWorkItems()
}

async function loadDashboardData() {
  loading.value = true
  mastersLoadError.value = false
  templatesLoadError.value = false

  const jobs: Promise<unknown>[] = []
  if (sessionStore.canAccessRoute('route.master-management')) {
    jobs.push(
      mastersStore.fetchMasters().catch(() => {
        mastersLoadError.value = true
      }),
    )
  }
  if (sessionStore.canAccessRoute('route.template-management')) {
    jobs.push(
      templatesStore.fetchTemplates().catch(() => {
        templatesLoadError.value = true
      }),
    )
  }
  if (canViewWorkItems.value && taskScope.value.fetchCollaboration) {
    jobs.push(
      fetchCollaborationWorkItems().catch(() => {
        /* error captured in collaboration store */
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
  void scrollToTasksIfRequested()
})

watch(
  () => [route.hash, route.query.queue, route.query.filter] as const,
  () => {
    void scrollToTasksIfRequested()
  },
)

watch(
  () => [route.query.queue, route.query.filter] as const,
  () => {
    void fetchCollaborationWorkItems().catch(() => {
      /* error captured in collaboration store */
    })
  },
)

async function scrollToTasksIfRequested() {
  if (route.hash !== '#tasks-section') {
    return
  }
  await nextTick()
  document.getElementById('tasks-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openTask(path: string) {
  router.push(path)
}

function openQuickLink(path: string) {
  router.push(path)
}
</script>

<template>
  <AppPageLayout class="dashboard-page" max-width="1200px">
    <header class="page-header">
      <h1>{{ t(taskScope.pageTitleKey) }}</h1>
      <p>{{ t(taskScope.pageDescriptionKey) }}</p>
    </header>

    <el-card shadow="never" class="summary-card">
      <h2>{{ t('home.summary.title') }}</h2>
      <dl class="summary-grid">
        <div>
          <dt>{{ t('home.summary.displayName') }}</dt>
          <dd>{{ sessionStore.session?.displayName }}</dd>
        </div>
        <div>
          <dt>{{ t('home.summary.authorizedGroups') }}</dt>
          <dd>{{ authorizedGroupsSummary }}</dd>
        </div>
      </dl>
    </el-card>

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

    <DashboardStatCards v-if="showStatsSection" :stats="stats" :loading="loading" />

    <section v-if="primaryClusterOneRole" id="journey-section" class="journey-section">
      <RoleJourneyTimeline
        :steps="journeySteps"
        :current-step-index="journeyCurrentStepIndex"
        :guidance-key="journeyGuidanceKey"
        :title-key="journeyTitleKey"
      />
    </section>

    <section id="tasks-section" class="tasks-section">
      <header class="section-header">
        <h2>{{ t('dashboard.tasks.title') }}</h2>
        <p>{{ t('dashboard.tasks.description') }}</p>
      </header>

      <el-skeleton v-if="loading || showCollaborationLoading" :rows="5" animated />

      <LoadErrorPanel
        v-if="collaborationFetchFailed"
        :message-key="collaborationLoadErrorKey"
        @retry="retryCollaborationLoad"
      />

      <template v-if="!loading && !showCollaborationLoading">
        <el-empty
          v-if="visiblePartitions.length === 0 && !collaborationFetchFailed"
          :description="t('dashboard.tasks.empty')"
        />

        <TaskHubPartitionSection
          v-for="partition in visiblePartitions"
          :key="partition.id"
          :partition="partition"
          :global-timeout-config="globalTimeoutConfig"
          :group-timeout-configs="groupTimeoutConfigs"
          @open="openTask"
        />
      </template>
    </section>

    <section v-if="quickLinks.length > 0" class="quick-links">
      <h2>{{ t('dashboard.quickLinks.title') }}</h2>
      <div class="quick-link-grid">
        <el-button
          v-for="link in quickLinks"
          :key="link.path"
          type="primary"
          plain
          @click="openQuickLink(link.path)"
        >
          {{ t(link.labelKey) }}
        </el-button>
      </div>
    </section>

    <CollaborationTimeoutConfigPanel v-if="showTimeoutConfig" />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-header {
  margin-bottom: 1.5rem;

  h1 {
    margin: 0 0 0.25rem;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.summary-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.1rem;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;

  dt {
    font-size: 0.8rem;
    color: var(--text-muted);
    margin-bottom: 0.25rem;
  }

  dd {
    margin: 0;
    font-weight: 600;
  }
}

.tasks-section {
  margin-bottom: 2rem;
}

.journey-section {
  margin-bottom: 2rem;
}

.section-header {
  margin-bottom: 1rem;

  h2 {
    margin: 0 0 0.25rem;
    font-size: 1.25rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.quick-links {
  h2 {
    margin: 0 0 1rem;
    font-size: 1.1rem;
  }
}

.quick-link-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}
</style>
