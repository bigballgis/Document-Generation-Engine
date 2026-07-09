import { onMounted, type ComputedRef, type Ref } from 'vue'
import type { PreviewRecord, TemplateDetail } from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import { useTemplateDetailTabs } from '@/views/templates/useTemplateDetailTabs'
import {
  useTemplateJourneyContext,
  type TemplateJourneyLifecycleDeps,
} from '@/views/templates/useTemplateJourneyContext'
import {
  useTemplateDetailLoad,
  type TemplateDetailLoadPolicyDeps,
} from '@/views/templates/useTemplateDetailLoad'

export type TemplateDetailNavigationLifecycleDeps = TemplateJourneyLifecycleDeps & {
  resetLifecycleTransientState: () => void
}

export type TemplateDetailNavigationPolicyDeps = TemplateDetailLoadPolicyDeps

export interface UseTemplateDetailNavigationOptions {
  isDevEditor: ComputedRef<boolean>
  templateId: ComputedRef<string>
  devVersionId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
  lastPreview: Ref<PreviewRecord | null>
  showAuthoringSection: ComputedRef<boolean>
  activeDetailTab: Ref<TemplateDetailTab>
  activeDevWorkspaceTab: Ref<TemplateDevWorkspaceTab>
  loadTemplateHolder: { fn: () => Promise<void> }
  lifecycle: TemplateDetailNavigationLifecycleDeps
  policy: TemplateDetailNavigationPolicyDeps
  handleTestGenerate: (testDataSetId?: string) => Promise<void>
}

export function useTemplateDetailNavigation(options: UseTemplateDetailNavigationOptions) {
  const {
    isDevEditor,
    templateId,
    devVersionId,
    template,
    lastPreview,
    showAuthoringSection,
    activeDetailTab,
    activeDevWorkspaceTab,
    loadTemplateHolder,
    lifecycle,
    policy,
    handleTestGenerate,
  } = options

  const tabs = useTemplateDetailTabs({
    isDevEditor,
    showAuthoringSection,
    showPolicyPanel: policy.showPolicyPanel,
    activeDetailTab,
    activeDevWorkspaceTab,
  })

  const journey = useTemplateJourneyContext({
    isDevEditor,
    template,
    lastPreview,
    lifecycle,
    openDevWorkspaceTab: tabs.openDevWorkspaceTab,
    openLifecyclePanel: tabs.openLifecyclePanel,
    handleTestGenerate,
  })

  const load = useTemplateDetailLoad({
    isDevEditor,
    templateId,
    devVersionId,
    activeDetailTab,
    activeDevWorkspaceTab,
    loadTemplateHolder,
    policy,
    lastPreview,
    resetLifecycleTransientState: lifecycle.resetLifecycleTransientState,
    resetJourneyEvidenceState: journey.resetJourneyEvidenceState,
  })

  onMounted(async () => {
    tabs.syncTabFromRoute()
    await Promise.all([load.loadTemplate(), load.loadAuthorRemediationWorkItems()])
  })

  return {
    activeDetailTab,
    activeDevWorkspaceTab,
    detailTabs: tabs.detailTabs,
    loadFailed: load.loadFailed,
    showAuthorJourney: journey.showAuthorJourney,
    authorJourneyContext: journey.authorJourneyContext,
    showTesterJourney: journey.showTesterJourney,
    testerJourneyContext: journey.testerJourneyContext,
    showApproverJourney: journey.showApproverJourney,
    approverJourneyContext: journey.approverJourneyContext,
    showTeamLeadJourney: journey.showTeamLeadJourney,
    teamLeadJourneyContext: journey.teamLeadJourneyContext,
    testerEvidenceViewed: journey.testerEvidenceViewed,
    approverEvidenceViewed: journey.approverEvidenceViewed,
    handleJourneyCreate: journey.handleJourneyCreate,
    handleJourneyDesign: journey.handleJourneyDesign,
    handleJourneyTrialGenerate: journey.handleJourneyTrialGenerate,
    handleJourneySubmitForTest: journey.handleJourneySubmitForTest,
    handleJourneySubmitForApproval: journey.handleJourneySubmitForApproval,
    handleJourneyReviewRequest: journey.handleJourneyReviewRequest,
    handleJourneyCheckEvidence: journey.handleJourneyCheckEvidence,
    handleJourneyRecordResult: journey.handleJourneyRecordResult,
    handleJourneyApproverReviewRequest: journey.handleJourneyApproverReviewRequest,
    handleJourneyApproverReviewSubmission: journey.handleJourneyApproverReviewSubmission,
    handleJourneyApproverRecordDecision: journey.handleJourneyApproverRecordDecision,
    handleJourneyTeamLeadReviewGoLiveRequest: journey.handleJourneyTeamLeadReviewGoLiveRequest,
    handleJourneyTeamLeadRunPreReleaseChecks: journey.handleJourneyTeamLeadRunPreReleaseChecks,
    handleJourneyTeamLeadConfirmGoLive: journey.handleJourneyTeamLeadConfirmGoLive,
    loadTemplate: load.loadTemplate,
    resetTransientDetailState: load.resetTransientDetailState,
    syncTabFromRoute: tabs.syncTabFromRoute,
    backToList: load.backToList,
    openLifecyclePanel: tabs.openLifecyclePanel,
    openDevWorkspaceTab: tabs.openDevWorkspaceTab,
    openTestPreviewTab: tabs.openTestPreviewTab,
    scrollToLifecyclePanel: tabs.scrollToLifecyclePanel,
    scrollToDevWorkspace: tabs.scrollToDevWorkspace,
  }
}
