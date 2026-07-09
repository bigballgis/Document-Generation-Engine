import { computed, ref, type ComputedRef, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCapabilities } from '@/composables/useCapabilities'
import { useCollaborationStore } from '@/stores/collaboration'
import { useSessionStore } from '@/stores/session'
import {
  isTemplateInRemediation,
  shouldShowTemplateAuthorJourney,
  type TemplateAuthorJourneyContext,
} from '@/utils/templateAuthorJourney'
import {
  shouldShowTemplateTesterJourney,
  type TemplateTesterJourneyContext,
} from '@/utils/templateTesterJourney'
import {
  shouldShowTemplateApproverJourney,
  type TemplateApproverJourneyContext,
} from '@/utils/templateApproverJourney'
import {
  shouldShowTemplateTeamLeadJourney,
  type TemplateTeamLeadJourneyContext,
} from '@/utils/templateTeamLeadJourney'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import type { PreviewRecord, TemplateDetail } from '@/types/template'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

export interface TemplateJourneyLifecycleDeps {
  publishGateReady: ComputedRef<boolean>
  submitForTestDialogOpen: Ref<boolean>
  handleSubmitForApproval: () => Promise<void>
  loadPublishGateData: () => Promise<void>
  handlePublish: () => Promise<void>
}

export interface UseTemplateJourneyContextOptions {
  isDevEditor: ComputedRef<boolean>
  template: ComputedRef<TemplateDetail | null>
  lastPreview: Ref<PreviewRecord | null>
  lifecycle: TemplateJourneyLifecycleDeps
  openDevWorkspaceTab: (tab: TemplateDevWorkspaceTab) => void
  openLifecyclePanel: () => void
  handleTestGenerate: (testDataSetId?: string) => Promise<void>
}

export function useTemplateJourneyContext(options: UseTemplateJourneyContextOptions) {
  const {
    isDevEditor,
    template,
    lastPreview,
    lifecycle,
    openDevWorkspaceTab,
    openLifecyclePanel,
    handleTestGenerate,
  } = options

  const router = useRouter()
  const sessionStore = useSessionStore()
  const collaborationStore = useCollaborationStore()
  const { authorTemplates, decideTests, decideApprovals, publishTemplates, reviewMasters } =
    useCapabilities()

  const testerEvidenceViewed = ref({
    fidelityViewedConfirmed: false,
    coverageViewedConfirmed: false,
    previewViewedConfirmed: false,
  })
  const approverEvidenceViewed = ref({
    submissionReviewedConfirmed: false,
    keyEvidenceViewedConfirmed: false,
  })
  const teamLeadGoLiveViewed = ref({
    goLiveRequestReviewedConfirmed: false,
    preReleaseChecksViewed: false,
  })

  const openRemediationTemplateIds = computed(() => {
    const ids = new Set<string>()
    for (const item of collaborationStore.workItems) {
      if (item.queue === 'REMEDIATION') {
        ids.add(item.templateId)
      }
    }
    return ids
  })

  const showAuthorJourney = computed(() => {
    if (
      !template.value ||
      !shouldShowTemplateAuthorJourney({
        authorTemplates: authorTemplates.value,
        roles: sessionStore.session?.roles ?? [],
      })
    ) {
      return false
    }
    const status = template.value.lifecycleStatus
    if (
      status === 'PENDING_RELEASE' &&
      publishTemplates.value &&
      shouldShowTemplateTeamLeadJourney({
        publishTemplates: publishTemplates.value,
        reviewMasters: reviewMasters.value,
      })
    ) {
      return false
    }
    return status !== 'STOPPED' && status !== 'DEPRECATED' && status !== 'DELETED'
  })

  const authorJourneyContext = computed((): TemplateAuthorJourneyContext | null => {
    if (!template.value) {
      return null
    }
    return {
      lifecycleStatus: template.value.lifecycleStatus,
      approvalSubState: template.value.approvalSubState ?? undefined,
      bindingsCount: template.value.bindings.length,
      hasSuccessfulTrialOutput: lastPreview.value?.status === 'SUCCEEDED',
      isRemediation: isTemplateInRemediation(template.value.id, openRemediationTemplateIds.value),
    }
  })

  const showTesterJourney = computed(() => {
    if (
      !template.value ||
      !shouldShowTemplateTesterJourney({ decideTests: decideTests.value }) ||
      template.value.lifecycleStatus !== 'TESTING'
    ) {
      return false
    }
    return true
  })

  const testerJourneyContext = computed((): TemplateTesterJourneyContext | null => {
    if (!template.value || template.value.lifecycleStatus !== 'TESTING') {
      return null
    }
    return {
      lifecycleStatus: 'TESTING',
      hasPreviewArtifact: lastPreview.value?.status === 'SUCCEEDED',
      fidelityViewedConfirmed: testerEvidenceViewed.value.fidelityViewedConfirmed,
      coverageViewedConfirmed: testerEvidenceViewed.value.coverageViewedConfirmed,
      previewViewedConfirmed: testerEvidenceViewed.value.previewViewedConfirmed,
    }
  })

  const showApproverJourney = computed(() => {
    if (
      !template.value ||
      !shouldShowTemplateApproverJourney({ decideApprovals: decideApprovals.value }) ||
      template.value.lifecycleStatus !== 'APPROVAL' ||
      template.value.approvalSubState !== 'PENDING_DECISION'
    ) {
      return false
    }
    return true
  })

  const approverJourneyContext = computed((): TemplateApproverJourneyContext | null => {
    if (
      !template.value ||
      template.value.lifecycleStatus !== 'APPROVAL' ||
      template.value.approvalSubState !== 'PENDING_DECISION'
    ) {
      return null
    }
    return {
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_DECISION',
      submissionReviewedConfirmed: approverEvidenceViewed.value.submissionReviewedConfirmed,
      keyEvidenceViewedConfirmed: approverEvidenceViewed.value.keyEvidenceViewedConfirmed,
    }
  })

  const showTeamLeadJourney = computed(() => {
    if (
      !template.value ||
      !publishTemplates.value ||
      template.value.lifecycleStatus !== 'PENDING_RELEASE'
    ) {
      return false
    }
    return shouldShowTemplateTeamLeadJourney({
      publishTemplates: publishTemplates.value,
      reviewMasters: reviewMasters.value,
    })
  })

  const teamLeadJourneyContext = computed((): TemplateTeamLeadJourneyContext | null => {
    if (!template.value || template.value.lifecycleStatus !== 'PENDING_RELEASE') {
      return null
    }
    return {
      lifecycleStatus: 'PENDING_RELEASE',
      goLiveRequestReviewedConfirmed: teamLeadGoLiveViewed.value.goLiveRequestReviewedConfirmed,
      preReleaseChecksViewed: teamLeadGoLiveViewed.value.preReleaseChecksViewed,
      publishGateReady: lifecycle.publishGateReady.value,
    }
  })

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
    showAuthorJourney,
    authorJourneyContext,
    showTesterJourney,
    testerJourneyContext,
    showApproverJourney,
    approverJourneyContext,
    showTeamLeadJourney,
    teamLeadJourneyContext,
    testerEvidenceViewed,
    approverEvidenceViewed,
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
