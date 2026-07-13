import type { ComputedRef, Ref } from 'vue'
import type { Router } from 'vue-router'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

export interface TemplateJourneyLifecycleDeps {
  publishGateReady: ComputedRef<boolean>
  submitForTestDialogOpen: Ref<boolean>
  handleSubmitForApproval: () => Promise<void>
  loadPublishGateData: () => Promise<void>
  handlePublish: () => Promise<void>
}

export interface UseTemplateJourneyHandlersOptions {
  isDevEditor: ComputedRef<boolean>
  router: Router
  lifecycle: TemplateJourneyLifecycleDeps
  openDevWorkspaceTab: (tab: TemplateDevWorkspaceTab) => void
  openLifecyclePanel: () => void
  handleTestGenerate: (testDataSetId?: string) => Promise<void>
  testerEvidenceViewed: Ref<{
    fidelityViewedConfirmed: boolean
    coverageViewedConfirmed: boolean
    previewViewedConfirmed: boolean
  }>
  approverEvidenceViewed: Ref<{
    submissionReviewedConfirmed: boolean
    keyEvidenceViewedConfirmed: boolean
  }>
  teamLeadGoLiveViewed: Ref<{
    goLiveRequestReviewedConfirmed: boolean
    preReleaseChecksViewed: boolean
  }>
}

export function useTemplateJourneyHandlers(options: UseTemplateJourneyHandlersOptions) {
  const {
    isDevEditor,
    router,
    lifecycle,
    openDevWorkspaceTab,
    openLifecyclePanel,
    handleTestGenerate,
    testerEvidenceViewed,
    approverEvidenceViewed,
    teamLeadGoLiveViewed,
  } = options

  function handleJourneyDesign() {
    openDevWorkspaceTab('design')
  }

  function handleJourneyCreate() {
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  }

  async function handleJourneyTrialGenerate() {
    openLifecyclePanel()
    await handleTestGenerate()
  }

  async function handleJourneySubmitForTest() {
    openDevWorkspaceTab('testing')
    lifecycle.submitForTestDialogOpen.value = true
  }

  async function handleJourneySubmitForApproval() {
    openLifecyclePanel()
    await lifecycle.handleSubmitForApproval()
  }

  function handleJourneyReviewRequest() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('testing')
      return
    }
    openLifecyclePanel()
  }

  function handleJourneyCheckEvidence() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('testing')
    } else {
      openLifecyclePanel()
    }
    testerEvidenceViewed.value = {
      fidelityViewedConfirmed: true,
      coverageViewedConfirmed: true,
      previewViewedConfirmed: false,
    }
  }

  function handleJourneyRecordResult() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('testing')
    } else {
      openLifecyclePanel()
    }
    testerEvidenceViewed.value = {
      fidelityViewedConfirmed: true,
      coverageViewedConfirmed: true,
      previewViewedConfirmed: true,
    }
  }

  function handleJourneyApproverReviewRequest() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('approval')
      return
    }
    openLifecyclePanel()
  }

  function handleJourneyApproverReviewSubmission() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('approval')
    } else {
      openLifecyclePanel()
    }
    approverEvidenceViewed.value = {
      ...approverEvidenceViewed.value,
      submissionReviewedConfirmed: true,
    }
  }

  function handleJourneyApproverRecordDecision() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('approval')
    } else {
      openLifecyclePanel()
    }
    approverEvidenceViewed.value = {
      submissionReviewedConfirmed: true,
      keyEvidenceViewedConfirmed: true,
    }
  }

  function handleJourneyTeamLeadReviewGoLiveRequest() {
    openLifecyclePanel()
    teamLeadGoLiveViewed.value = {
      ...teamLeadGoLiveViewed.value,
      goLiveRequestReviewedConfirmed: true,
    }
  }

  async function handleJourneyTeamLeadRunPreReleaseChecks() {
    openLifecyclePanel()
    await lifecycle.loadPublishGateData()
    teamLeadGoLiveViewed.value = {
      ...teamLeadGoLiveViewed.value,
      goLiveRequestReviewedConfirmed: true,
      preReleaseChecksViewed: true,
    }
  }

  async function handleJourneyTeamLeadConfirmGoLive() {
    openLifecyclePanel()
    teamLeadGoLiveViewed.value = {
      goLiveRequestReviewedConfirmed: true,
      preReleaseChecksViewed: true,
    }
    await lifecycle.handlePublish()
  }

  function resetJourneyEvidenceState() {
    testerEvidenceViewed.value = {
      fidelityViewedConfirmed: false,
      coverageViewedConfirmed: false,
      previewViewedConfirmed: false,
    }
    approverEvidenceViewed.value = {
      submissionReviewedConfirmed: false,
      keyEvidenceViewedConfirmed: false,
    }
    teamLeadGoLiveViewed.value = {
      goLiveRequestReviewedConfirmed: false,
      preReleaseChecksViewed: false,
    }
  }

  return {
    handleJourneyCreate,
    handleJourneyDesign,
    handleJourneyTrialGenerate,
    handleJourneySubmitForTest,
    handleJourneySubmitForApproval,
    handleJourneyReviewRequest,
    handleJourneyCheckEvidence,
    handleJourneyRecordResult,
    handleJourneyApproverReviewRequest,
    handleJourneyApproverReviewSubmission,
    handleJourneyApproverRecordDecision,
    handleJourneyTeamLeadReviewGoLiveRequest,
    handleJourneyTeamLeadRunPreReleaseChecks,
    handleJourneyTeamLeadConfirmGoLive,
    resetJourneyEvidenceState,
  }
}
