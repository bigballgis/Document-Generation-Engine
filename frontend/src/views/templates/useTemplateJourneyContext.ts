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
import type { PreviewRecord, TemplateDetail } from '@/types/template'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import {
  useTemplateJourneyHandlers,
  type TemplateJourneyLifecycleDeps,
} from '@/views/templates/useTemplateJourneyHandlers'

export type { TemplateJourneyLifecycleDeps }

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

  const handlers = useTemplateJourneyHandlers({
    isDevEditor,
    router,
    lifecycle,
    openDevWorkspaceTab,
    openLifecyclePanel,
    handleTestGenerate,
    testerEvidenceViewed,
    approverEvidenceViewed,
    teamLeadGoLiveViewed,
  })

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
    ...handlers,
  }
}
