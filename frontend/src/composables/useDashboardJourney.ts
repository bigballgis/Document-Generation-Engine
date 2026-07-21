import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  resolveClusterOneJourney,
  resolvePrimaryClusterOneRole,
  roleJourneyTitleKey,
  globalAdminJourneySteps,
  templateLegalReviewerJourneySteps,
  templateTeamLeadJourneySteps,
} from '@/constants/roleJourneyDefinitions'
import {
  shouldShowTemplateLegalReviewerJourney,
} from '@/utils/templateLegalReviewerJourney'
import {
  shouldShowTemplateTeamLeadJourney,
} from '@/utils/templateTeamLeadJourney'
import {
  shouldShowGlobalAdminJourney,
} from '@/utils/globalAdminJourney'
import { canMaintainCollaborationTimeoutConfig, MANAGEMENT_ROLES } from '@/auth/roles'
import { useCapabilities } from '@/composables/useCapabilities'
import { useDashboardJourneyResolutions } from '@/composables/useDashboardJourneyResolutions'
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
  const {
    context,
    decideLegalApprovals,
    publishTemplates,
    reviewMasters,
    deleteTemplates,
  } = useCapabilities()

  const showTimeoutConfig = computed(() => canMaintainCollaborationTimeoutConfig(context.value))

  const primaryClusterOneRole = computed(() =>
    resolvePrimaryClusterOneRole(sessionStore.session?.roles ?? []),
  )

  const showLegalReviewerJourney = computed(
    () =>
      !primaryClusterOneRole.value &&
      (sessionStore.session?.roles ?? []).includes(MANAGEMENT_ROLES.LEGAL_REVIEWER) &&
      shouldShowTemplateLegalReviewerJourney({
        decideLegalApprovals: decideLegalApprovals.value,
      }),
  )

  /** Retired TEMPLATE_APPROVER-only dashboard path — compliance decide is capability-gated on template UI. */
  const showApproverJourney = computed(() => false)

  const showGlobalAdminJourney = computed(
    () =>
      !primaryClusterOneRole.value &&
      !showLegalReviewerJourney.value &&
      !showApproverJourney.value &&
      shouldShowGlobalAdminJourney({ roles: sessionStore.session?.roles ?? [] }),
  )

  const showTeamLeadJourney = computed(
    () =>
      !primaryClusterOneRole.value &&
      !showLegalReviewerJourney.value &&
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
          showLegalReviewerJourney.value ||
          showApproverJourney.value ||
          showGlobalAdminJourney.value ||
          showTeamLeadJourney.value,
      ),
  )

  const journeySteps = computed(() => {
    if (primaryClusterOneRole.value) {
      return resolveClusterOneJourney(primaryClusterOneRole.value)
    }
    if (showLegalReviewerJourney.value) {
      return templateLegalReviewerJourneySteps
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
    if (showLegalReviewerJourney.value) {
      return roleJourneyTitleKey('LEGAL_REVIEWER')
    }
    if (showGlobalAdminJourney.value) {
      return roleJourneyTitleKey('GLOBAL_ADMIN')
    }
    if (showTeamLeadJourney.value) {
      return roleJourneyTitleKey('GROUP_ADMIN')
    }
    return undefined
  })

  const { journeyCurrentStepIndex, journeyGuidanceKey, dashboardJourneyPath } =
    useDashboardJourneyResolutions(
      {
        primaryClusterOneRole,
        showLegalReviewerJourney,
        showApproverJourney,
        showGlobalAdminJourney,
        showTeamLeadJourney,
        showTimeoutConfig,
        deleteTemplates,
      },
      { mastersStore, templatesStore, collaborationStore },
    )

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
