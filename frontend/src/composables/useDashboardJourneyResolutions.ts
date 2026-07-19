import { computed } from 'vue'
import {
  buildDashboardJourneyPath,
  type DashboardJourneyKind,
} from '@/utils/dashboardJourneyNavigation'
import type { useCollaborationStore } from '@/stores/collaboration'
import type { useMastersStore } from '@/stores/masters'
import type { useTemplatesStore } from '@/stores/templates'
import {
  useDashboardJourneyRoleResolutions,
  type DashboardJourneyVisibility,
} from '@/composables/useDashboardJourneyRoleResolutions'

export type { DashboardJourneyVisibility } from '@/composables/useDashboardJourneyRoleResolutions'

type CollaborationStore = ReturnType<typeof useCollaborationStore>
type MastersStore = ReturnType<typeof useMastersStore>
type TemplatesStore = ReturnType<typeof useTemplatesStore>

export function useDashboardJourneyResolutions(
  visibility: DashboardJourneyVisibility,
  stores: {
    mastersStore: MastersStore
    templatesStore: TemplatesStore
    collaborationStore: CollaborationStore
  },
) {
  const {
    primaryClusterOneRole,
    showLegalReviewerJourney,
    showApproverJourney,
    showGlobalAdminJourney,
    showTeamLeadJourney,
  } = visibility

  const {
    masterDesignerJourneyResolution,
    templateAuthorJourneyResolution,
    templateTesterJourneyResolution,
    templateLegalReviewerJourneyResolution,
    templateApproverJourneyResolution,
    templateTeamLeadJourneyResolution,
    globalAdminJourneyResolution,
  } = useDashboardJourneyRoleResolutions(visibility, stores)

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
    if (showLegalReviewerJourney.value) {
      return templateLegalReviewerJourneyResolution.value?.currentStepIndex ?? null
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
    if (showLegalReviewerJourney.value) {
      return templateLegalReviewerJourneyResolution.value?.guidanceKey
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
    if (showLegalReviewerJourney.value) {
      return 'LEGAL_REVIEWER'
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
    if (showLegalReviewerJourney.value) {
      return templateLegalReviewerJourneyResolution.value?.activeStepId
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
    if (showLegalReviewerJourney.value) {
      return templateLegalReviewerJourneyResolution.value?.targetTemplateId
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
