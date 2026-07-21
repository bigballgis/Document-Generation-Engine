import { computed, type ComputedRef } from 'vue'
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
  resolveTemplateLegalReviewerDashboardJourneyIndex,
  type TemplateLegalApprovalWorkItem,
} from '@/utils/templateLegalReviewerJourney'
import {
  resolveTemplateTeamLeadDashboardJourneyIndex,
  type TemplateTeamLeadPendingReleaseWorkItem,
} from '@/utils/templateTeamLeadJourney'
import {
  resolveGlobalAdminDashboardJourneyIndex,
  type GlobalAdminCollaborationWorkItem,
} from '@/utils/globalAdminJourney'
import type { useCollaborationStore } from '@/stores/collaboration'
import type { useMastersStore } from '@/stores/masters'
import type { useTemplatesStore } from '@/stores/templates'
import type { ClusterOneRole } from '@/constants/roleJourneyDefinitions'

type CollaborationStore = ReturnType<typeof useCollaborationStore>
type MastersStore = ReturnType<typeof useMastersStore>
type TemplatesStore = ReturnType<typeof useTemplatesStore>

export type DashboardJourneyVisibility = {
  primaryClusterOneRole: ComputedRef<ClusterOneRole | null>
  showLegalReviewerJourney: ComputedRef<boolean>
  showApproverJourney: ComputedRef<boolean>
  showGlobalAdminJourney: ComputedRef<boolean>
  showTeamLeadJourney: ComputedRef<boolean>
  showTimeoutConfig: ComputedRef<boolean>
  deleteTemplates: ComputedRef<boolean>
}

export function useDashboardJourneyRoleResolutions(
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
  const { mastersStore, templatesStore, collaborationStore } = stores

  /** Letterhead journey stays on master hub pages; dashboard cluster-① uses DOCUMENT_AUTHOR authoring. */
  const masterDesignerJourneyResolution = computed(() => null)

  const templateAuthorRemediationItems = computed((): TemplateAuthorRemediationItem[] =>
    collaborationStore.workItems
      .filter((item) => item.queue === 'REMEDIATION')
      .map((item) => ({
        templateId: item.templateId,
        createdAt: item.createdAt,
      })),
  )

  const templateAuthorJourneyResolution = computed(() => {
    if (primaryClusterOneRole.value !== 'DOCUMENT_AUTHOR') {
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

  const templateLegalApprovalWorkItems = computed((): TemplateLegalApprovalWorkItem[] =>
    collaborationStore.workItems
      .filter((item) => item.queue === 'LEGAL')
      .map((item) => ({
        templateId: item.templateId,
        createdAt: item.createdAt,
      })),
  )

  const templateLegalReviewerJourneyResolution = computed(() => {
    if (!showLegalReviewerJourney.value) {
      return null
    }
    return resolveTemplateLegalReviewerDashboardJourneyIndex(
      templatesStore.templates,
      templateLegalApprovalWorkItems.value,
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

  return {
    masterDesignerJourneyResolution,
    templateAuthorJourneyResolution,
    templateTesterJourneyResolution,
    templateLegalReviewerJourneyResolution,
    templateApproverJourneyResolution,
    templateTeamLeadJourneyResolution,
    globalAdminJourneyResolution,
  }
}
