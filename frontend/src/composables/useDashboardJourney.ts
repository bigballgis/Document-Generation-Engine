import { computed } from 'vue'
import { useRouter } from 'vue-router'
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
import { canMaintainCollaborationTimeoutConfig, MANAGEMENT_ROLES } from '@/auth/roles'
import { useCapabilities } from '@/composables/useCapabilities'
import {
  buildDashboardJourneyPath,
  type DashboardJourneyKind,
} from '@/utils/dashboardJourneyNavigation'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'

export function useDashboardJourney() {
  const router = useRouter()
  const sessionStore = useSessionStore()
  const mastersStore = useMastersStore()
  const templatesStore = useTemplatesStore()
  const collaborationStore = useCollaborationStore()
  const { context, decideApprovals, publishTemplates, reviewMasters, deleteTemplates } =
    useCapabilities()

  const showTimeoutConfig = computed(() => canMaintainCollaborationTimeoutConfig(context.value))

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

  function openDashboardJourney() {
    if (!dashboardJourneyPath.value) {
      return
    }
    router.push(dashboardJourneyPath.value)
  }

  return {
    primaryClusterOneRole,
    showJourneySection,
    showTimeoutConfig,
    journeySteps,
    journeyTitleKey,
    journeyCurrentStepIndex,
    journeyGuidanceKey,
    dashboardJourneyPath,
    openDashboardJourney,
  }
}
