import { computed, type ComputedRef } from 'vue'
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
  type TemplateApproverApprovalWorkItem,
} from '@/utils/templateApproverJourney'
import {
  resolveTemplateTeamLeadDashboardJourneyIndex,
  type TemplateTeamLeadPendingReleaseWorkItem,
} from '@/utils/templateTeamLeadJourney'
import {
  resolveGlobalAdminDashboardJourneyIndex,
  type GlobalAdminCollaborationWorkItem,
} from '@/utils/globalAdminJourney'
import {
  buildDashboardJourneyPath,
  type DashboardJourneyKind,
} from '@/utils/dashboardJourneyNavigation'
import type { useCollaborationStore } from '@/stores/collaboration'
import type { useMastersStore } from '@/stores/masters'
import type { useTemplatesStore } from '@/stores/templates'
import type { ClusterOneRole } from '@/constants/roleJourneyDefinitions'

type CollaborationStore = ReturnType<typeof useCollaborationStore>
type MastersStore = ReturnType<typeof useMastersStore>
type TemplatesStore = ReturnType<typeof useTemplatesStore>

export type DashboardJourneyVisibility = {
  primaryClusterOneRole: ComputedRef<ClusterOneRole | null>
  showApproverJourney: ComputedRef<boolean>
  showGlobalAdminJourney: ComputedRef<boolean>
  showTeamLeadJourney: ComputedRef<boolean>
  showTimeoutConfig: ComputedRef<boolean>
  deleteTemplates: ComputedRef<boolean>
}

export function useDashboardJourneyResolutions(
  visibility: DashboardJourneyVisibility,
  stores: {
    mastersStore: MastersStore
    templatesStore: TemplatesStore
    collaborationStore: CollaborationStore
  },
) {
  const { primaryClusterOneRole, showApproverJourney, showGlobalAdminJourney, showTeamLeadJourney } =
    visibility
  const { mastersStore, templatesStore, collaborationStore } = stores

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

  const templateTeamLeadPendingReleaseWorkItems = computed(
    (): TemplateTeamLeadPendingReleaseWorkItem[] =>
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
        deleteTemplates: visibility.deleteTemplates.value,
        canMaintainCollaborationTimeoutConfig: visibility.showTimeoutConfig.value,
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

  return {
    journeyCurrentStepIndex,
    journeyGuidanceKey,
    dashboardJourneyPath,
  }
}
