<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import DashboardStatCards from '@/components/dashboard/DashboardStatCards.vue'
import TaskHubPartitionSection from '@/components/dashboard/TaskHubPartitionSection.vue'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import {
  resolveClusterOneJourney,
  resolvePrimaryClusterOneRole,
  roleJourneyTitleKey,
  globalAdminJourneySteps,
  templateApproverJourneySteps,
  templateTeamLeadJourneySteps,
} from '@/constants/roleJourneyDefinitions'
import {
  resolveMasterDesignerDashboardJourneyIndex,
  type MasterDesignerDashboardMaster,
} from '@/utils/masterDesignerJourney'
import {
  resolveTemplateAuthorDashboardJourneyIndex,
  type TemplateAuthorRemediationItem,
} from '@/utils/templateAuthorJourney'
import {
  resolveTemplateTesterDashboardJourneyIndex,
  type TemplateTesterTestWorkItem,
} from '@/utils/templateTesterJourney'
import {
  resolveTemplateApproverDashboardJourneyIndex,
  shouldShowTemplateApproverJourney,
  type TemplateApproverApprovalWorkItem,
} from '@/utils/templateApproverJourney'
import {
  resolveTemplateTeamLeadDashboardJourneyIndex,
  shouldShowTemplateTeamLeadJourney,
  type TemplateTeamLeadPendingReleaseWorkItem,
} from '@/utils/templateTeamLeadJourney'
import {
  resolveGlobalAdminDashboardJourneyIndex,
  shouldShowGlobalAdminJourney,
  type GlobalAdminCollaborationWorkItem,
} from '@/utils/globalAdminJourney'
import { useDashboardStats } from '@/composables/useDashboardStats'
import {
  buildTaskPartitions,
  dashboardQuickLinks,
  parseDashboardTaskScope,
  useWorkflowTasks,
} from '@/composables/useWorkflowTasks'
import { canMaintainCollaborationTimeoutConfig, canViewCollaborationWorkItems, MANAGEMENT_ROLES } from '@/auth/roles'
import { useCapabilities } from '@/composables/useCapabilities'
import {
  buildDashboardJourneyPath,
  type DashboardJourneyKind,
} from '@/utils/dashboardJourneyNavigation'
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
const { context, reviewMasters, manageMasters, decideApprovals, publishTemplates, deleteTemplates } =
  useCapabilities()
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

const pageDescription = computed(() => {
  const base = t(taskScope.value.pageDescriptionKey)
  return `${base} ${t('home.summary.authorizedGroups')}: ${authorizedGroupsSummary.value}`
})

const canViewWorkItems = computed(() => canViewCollaborationWorkItems(context.value))

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

const showApproverJourney = computed(
  () =>
    !primaryClusterOneRole.value &&
    (sessionStore.session?.roles ?? []).includes(MANAGEMENT_ROLES.TEMPLATE_APPROVER) &&
    shouldShowTemplateApproverJourney({ decideApprovals: decideApprovals.value }),
)

const showGlobalAdminJourney = computed(
  () =>
    !primaryClusterOneRole.value &&
    !showApproverJourney.value &&
    shouldShowGlobalAdminJourney({ roles: sessionStore.session?.roles ?? [] }),
)

const showTeamLeadJourney = computed(
  () =>
    !primaryClusterOneRole.value &&
    !showApproverJourney.value &&
    !showGlobalAdminJourney.value &&
    (sessionStore.session?.roles ?? []).includes(MANAGEMENT_ROLES.GROUP_ADMIN) &&
    shouldShowTemplateTeamLeadJourney({
      publishTemplates: publishTemplates.value,
      reviewMasters: reviewMasters.value,
    }),
)

const showJourneySection = computed(
  () =>
    Boolean(
      primaryClusterOneRole.value ||
        showApproverJourney.value ||
        showGlobalAdminJourney.value ||
        showTeamLeadJourney.value,
    ),
)

const journeySteps = computed(() => {
  if (primaryClusterOneRole.value) {
    return resolveClusterOneJourney(primaryClusterOneRole.value)
  }
  if (showApproverJourney.value) {
    return templateApproverJourneySteps
  }
  if (showGlobalAdminJourney.value) {
    return globalAdminJourneySteps
  }
  if (showTeamLeadJourney.value) {
    return templateTeamLeadJourneySteps
  }
  return []
})

const journeyTitleKey = computed(() => {
  if (primaryClusterOneRole.value) {
    return roleJourneyTitleKey(primaryClusterOneRole.value)
  }
  if (showApproverJourney.value) {
    return roleJourneyTitleKey('TEMPLATE_APPROVER')
  }
  if (showGlobalAdminJourney.value) {
    return roleJourneyTitleKey('GLOBAL_ADMIN')
  }
  if (showTeamLeadJourney.value) {
    return roleJourneyTitleKey('GROUP_ADMIN')
  }
  return undefined
})

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

const templateAuthorRemediationItems = computed((): TemplateAuthorRemediationItem[] =>
  collaborationStore.workItems
    .filter((item) => item.queue === 'REMEDIATION')
    .map((item) => ({
      templateId: item.templateId,
      createdAt: item.createdAt,
    })),
)

const templateAuthorJourneyResolution = computed(() => {
  if (primaryClusterOneRole.value !== 'TEMPLATE_AUTHOR') {
    return null
  }
  return resolveTemplateAuthorDashboardJourneyIndex(
    templatesStore.templates,
    templateAuthorRemediationItems.value,
  )
})

const templateTesterTestWorkItems = computed((): TemplateTesterTestWorkItem[] =>
  collaborationStore.workItems
    .filter((item) => item.queue === 'TEST')
    .map((item) => ({
      templateId: item.templateId,
      createdAt: item.createdAt,
    })),
)

const templateTesterJourneyResolution = computed(() => {
  if (primaryClusterOneRole.value !== 'TEMPLATE_TESTER') {
    return null
  }
  return resolveTemplateTesterDashboardJourneyIndex(
    templatesStore.templates,
    templateTesterTestWorkItems.value,
  )
})

const templateApproverApprovalWorkItems = computed((): TemplateApproverApprovalWorkItem[] =>
  collaborationStore.workItems
    .filter((item) => item.queue === 'APPROVAL')
    .map((item) => ({
      templateId: item.templateId,
      createdAt: item.createdAt,
    })),
)

const templateApproverJourneyResolution = computed(() => {
  if (!showApproverJourney.value) {
    return null
  }
  return resolveTemplateApproverDashboardJourneyIndex(
    templatesStore.templates,
    templateApproverApprovalWorkItems.value,
  )
})

const templateTeamLeadPendingReleaseWorkItems = computed((): TemplateTeamLeadPendingReleaseWorkItem[] =>
  collaborationStore.workItems
    .filter((item) => item.queue === 'PENDING_RELEASE')
    .map((item) => ({
      templateId: item.templateId,
      createdAt: item.createdAt,
    })),
)

const templateTeamLeadJourneyResolution = computed(() => {
  if (!showTeamLeadJourney.value) {
    return null
  }
  return resolveTemplateTeamLeadDashboardJourneyIndex(
    mastersStore.masters,
    templatesStore.templates,
    templateTeamLeadPendingReleaseWorkItems.value,
  )
})

const globalAdminCollaborationWorkItems = computed((): GlobalAdminCollaborationWorkItem[] =>
  collaborationStore.workItems.map((item) => ({
    queue: item.queue,
    createdAt: item.createdAt,
  })),
)

const globalAdminJourneyResolution = computed(() => {
  if (!showGlobalAdminJourney.value) {
    return null
  }
  return resolveGlobalAdminDashboardJourneyIndex(
    mastersStore.masters,
    templatesStore.templates,
    globalAdminCollaborationWorkItems.value,
    {
      deleteTemplates: deleteTemplates.value,
      canMaintainCollaborationTimeoutConfig: showTimeoutConfig.value,
    },
  )
})

const journeyCurrentStepIndex = computed(() => {
  if (primaryClusterOneRole.value === 'MASTER_DESIGNER') {
    return masterDesignerJourneyResolution.value?.currentStepIndex ?? null
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_AUTHOR') {
    return templateAuthorJourneyResolution.value?.currentStepIndex ?? null
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_TESTER') {
    return templateTesterJourneyResolution.value?.currentStepIndex ?? null
  }
  if (showApproverJourney.value) {
    return templateApproverJourneyResolution.value?.currentStepIndex ?? null
  }
  if (showGlobalAdminJourney.value) {
    return globalAdminJourneyResolution.value?.currentStepIndex ?? null
  }
  if (showTeamLeadJourney.value) {
    return templateTeamLeadJourneyResolution.value?.currentStepIndex ?? null
  }
  return null
})

const journeyGuidanceKey = computed(() => {
  if (primaryClusterOneRole.value === 'MASTER_DESIGNER') {
    return masterDesignerJourneyResolution.value?.guidanceKey
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_AUTHOR') {
    return templateAuthorJourneyResolution.value?.guidanceKey
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_TESTER') {
    return templateTesterJourneyResolution.value?.guidanceKey
  }
  if (showApproverJourney.value) {
    return templateApproverJourneyResolution.value?.guidanceKey
  }
  if (showGlobalAdminJourney.value) {
    return globalAdminJourneyResolution.value?.guidanceKey
  }
  if (showTeamLeadJourney.value) {
    return templateTeamLeadJourneyResolution.value?.guidanceKey
  }
  return undefined
})

const dashboardJourneyKind = computed((): DashboardJourneyKind | null => {
  if (primaryClusterOneRole.value === 'MASTER_DESIGNER') {
    return 'MASTER_DESIGNER'
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_AUTHOR') {
    return 'TEMPLATE_AUTHOR'
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_TESTER') {
    return 'TEMPLATE_TESTER'
  }
  if (showApproverJourney.value) {
    return 'TEMPLATE_APPROVER'
  }
  if (showGlobalAdminJourney.value) {
    return 'GLOBAL_ADMIN'
  }
  if (showTeamLeadJourney.value) {
    return 'GROUP_ADMIN'
  }
  return null
})

const journeyActiveStepId = computed(() => {
  if (primaryClusterOneRole.value === 'MASTER_DESIGNER') {
    return masterDesignerJourneyResolution.value?.activeStepId
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_AUTHOR') {
    return templateAuthorJourneyResolution.value?.activeStepId
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_TESTER') {
    return templateTesterJourneyResolution.value?.activeStepId
  }
  if (showApproverJourney.value) {
    return templateApproverJourneyResolution.value?.activeStepId
  }
  if (showGlobalAdminJourney.value) {
    return globalAdminJourneyResolution.value?.activeStepId
  }
  if (showTeamLeadJourney.value) {
    return templateTeamLeadJourneyResolution.value?.activeStepId
  }
  return undefined
})

const journeyTargetTemplateId = computed(() => {
  if (primaryClusterOneRole.value === 'TEMPLATE_AUTHOR') {
    return templateAuthorJourneyResolution.value?.targetTemplateId
  }
  if (primaryClusterOneRole.value === 'TEMPLATE_TESTER') {
    return templateTesterJourneyResolution.value?.targetTemplateId
  }
  if (showApproverJourney.value) {
    return templateApproverJourneyResolution.value?.targetTemplateId
  }
  if (showTeamLeadJourney.value) {
    return templateTeamLeadJourneyResolution.value?.targetTemplateId
  }
  return undefined
})

const journeyTargetMasterId = computed(() => {
  if (primaryClusterOneRole.value === 'MASTER_DESIGNER') {
    return masterDesignerJourneyResolution.value?.targetMasterId
  }
  if (showTeamLeadJourney.value) {
    return templateTeamLeadJourneyResolution.value?.targetMasterId
  }
  return undefined
})

const dashboardJourneyPath = computed(() => {
  const kind = dashboardJourneyKind.value
  if (!kind) {
    return null
  }
  return buildDashboardJourneyPath({
    kind,
    activeStepId: journeyActiveStepId.value,
    targetTemplateId: journeyTargetTemplateId.value,
    targetMasterId: journeyTargetMasterId.value,
  })
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

  if (sessionStore.hasRole(MANAGEMENT_ROLES.GLOBAL_ADMIN)) {
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

function openDashboardJourney() {
  if (!dashboardJourneyPath.value) {
    return
  }
  router.push(dashboardJourneyPath.value)
}
</script>

<template>
  <AppPageLayout>
    <PageHeader
      :title="t(taskScope.pageTitleKey)"
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

    <DashboardStatCards v-if="showStatsSection" :stats="stats" :loading="loading" />

    <section v-if="showJourneySection" id="journey-section" class="journey-section">
      <RoleJourneyTimeline
        :steps="journeySteps"
        :current-step-index="journeyCurrentStepIndex"
        :guidance-key="journeyGuidanceKey"
        :title-key="journeyTitleKey"
      >
        <template v-if="dashboardJourneyPath" #after>
          <el-button
            link
            type="primary"
            data-dashboard-journey-link
            @click="openDashboardJourney"
          >
            {{ t('dashboard.journey.openWorkspace') }}
          </el-button>
        </template>
      </RoleJourneyTimeline>
    </section>

    <section id="tasks-section" class="tasks-section">
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
          link
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
.tasks-section {
  margin-bottom: 2rem;
}

.journey-section {
  margin-bottom: 2rem;
}

.section-header {
  margin-bottom: 1rem;
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
