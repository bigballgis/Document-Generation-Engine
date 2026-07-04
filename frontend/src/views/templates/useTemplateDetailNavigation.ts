import { computed, nextTick, onMounted, ref, watch, type ComputedRef, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { canViewCollaborationWorkItems } from '@/auth/roles'
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
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import {
  normalizeTemplateDetailQuery,
  resolveTemplateDetailTab,
  resolveTemplateDetailTabFromQuery,
  templateDetailTabLabelKey,
  type TemplateDetailTab,
} from '@/views/templates/templateDetailTabs'
import {
  buildDevWorkspaceQuery,
  resolveTemplateDevWorkspaceTabFromQuery,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import type { PreviewRecord, TemplateDetail } from '@/types/template'

export interface TemplateDetailNavigationLifecycleDeps {
  publishGateReady: ComputedRef<boolean>
  submitForTestDialogOpen: Ref<boolean>
  handleSubmitForApproval: () => Promise<void>
  loadPublishGateData: () => Promise<void>
  handlePublish: () => Promise<void>
  resetLifecycleTransientState: () => void
}

export interface TemplateDetailNavigationPolicyDeps {
  showPolicyPanel: ComputedRef<boolean>
  loadPolicyData: () => Promise<void>
  resetPolicyCredentialsTransientState: () => void
}

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

  const route = useRoute()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const sessionStore = useSessionStore()
  const collaborationStore = useCollaborationStore()
  const {
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    context,
  } = useCapabilities()

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
  const loadFailed = ref(false)

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
      approvalSubState: template.value.approvalSubState,
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

  const detailTabs = computed(() => {
    const tabs = [
      { name: 'overview', labelKey: templateDetailTabLabelKey('overview') },
      { name: 'lifecycle', labelKey: templateDetailTabLabelKey('lifecycle') },
    ] as Array<{ name: string; labelKey: string }>
    if (showAuthoringSection.value) {
      tabs.push({ name: 'authoring', labelKey: templateDetailTabLabelKey('authoring') })
    }
    tabs.push({ name: 'releaseVersions', labelKey: templateDetailTabLabelKey('releaseVersions') })
    if (policy.showPolicyPanel.value) {
      tabs.push({ name: 'apiAccess', labelKey: templateDetailTabLabelKey('apiAccess') })
    }
    return tabs
  })

  function scrollToLifecyclePanel() {
    void nextTick(() => {
      document.getElementById('template-lifecycle-panel')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    })
  }

  function scrollToDevWorkspace() {
    void nextTick(() => {
      document.getElementById('dev-workspace')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    })
  }

  function openDevWorkspaceTab(tab: TemplateDevWorkspaceTab) {
    activeDevWorkspaceTab.value = tab
    void router.replace({ query: buildDevWorkspaceQuery(route.query, tab) })
    scrollToDevWorkspace()
  }

  function openLifecyclePanel() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('approval')
      return
    }
    activeDetailTab.value = 'lifecycle'
    scrollToLifecyclePanel()
  }

  function openTestPreviewTab() {
    openDevWorkspaceTab('testing')
  }

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
    openTestPreviewTab()
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

  function resetTransientDetailState() {
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
    lifecycle.resetLifecycleTransientState()
    policy.resetPolicyCredentialsTransientState()
    lastPreview.value = null
  }

  function syncTabFromRoute() {
    const normalized = normalizeTemplateDetailQuery(route.query)
    if (normalized) {
      if (isDevEditor.value) {
        activeDevWorkspaceTab.value = 'approval'
        scrollToDevWorkspace()
        void router.replace({
          query: buildDevWorkspaceQuery(normalized.query, 'approval'),
        })
        return
      }
      activeDetailTab.value = normalized.tab
      scrollToLifecyclePanel()
      void router.replace({ query: normalized.query })
      return
    }

    if (isDevEditor.value) {
      const workspaceTab = resolveTemplateDevWorkspaceTabFromQuery(route.query)
      if (activeDevWorkspaceTab.value !== workspaceTab) {
        activeDevWorkspaceTab.value = workspaceTab
      }
      if (route.query.focus === 'workflow') {
        scrollToDevWorkspace()
      }
      return
    }

    const tab = resolveTemplateDetailTabFromQuery(route.query)
    if (activeDetailTab.value !== tab) {
      activeDetailTab.value = tab
    }
  }

  async function loadAuthorRemediationWorkItems() {
    if (!authorTemplates.value || !canViewCollaborationWorkItems(context.value)) {
      return
    }
    try {
      await collaborationStore.fetchWorkItems({ queue: 'REMEDIATION' })
    } catch {
      /* degrade — remediation flag may be false until work items load */
    }
  }

  async function loadTemplate() {
    loadFailed.value = false
    try {
      if (isDevEditor.value && devVersionId.value) {
        const detail = await templatesApi.fetchDevVersionDetail(templateId.value, devVersionId.value)
        templatesStore.$patch({ selectedTemplate: detail })
      } else {
        await templatesStore.fetchTemplate(templateId.value)
      }
      if (policy.showPolicyPanel.value) {
        await policy.loadPolicyData()
      }
    } catch {
      loadFailed.value = true
    }
  }

  loadTemplateHolder.fn = loadTemplate

  function backToList() {
    if (isDevEditor.value) {
      router.push(templatePackageHubPath(templateId.value))
      return
    }
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  }

  onMounted(async () => {
    syncTabFromRoute()
    await Promise.all([loadTemplate(), loadAuthorRemediationWorkItems()])
  })

  watch(
    () => devVersionId.value,
    () => {
      if (isDevEditor.value) {
        resetTransientDetailState()
        void loadTemplate()
      }
    },
  )

  watch(
    () => templateId.value,
    () => {
      resetTransientDetailState()
      activeDetailTab.value = isDevEditor.value
        ? 'authoring'
        : resolveTemplateDetailTabFromQuery(route.query)
      if (isDevEditor.value) {
        activeDevWorkspaceTab.value = resolveTemplateDevWorkspaceTabFromQuery(route.query)
      }
      void loadTemplate()
    },
  )

  watch(
    () => route.query,
    () => {
      syncTabFromRoute()
    },
    { deep: true },
  )

  watch(activeDetailTab, (tab) => {
    if (route.query.focus === 'lifecycle' || route.query.focus === 'workflow') {
      return
    }
    if (isDevEditor.value && tab === 'lifecycle') {
      return
    }
    if (resolveTemplateDetailTab(route.query.tab) === tab) {
      return
    }
    const query = { ...route.query }
    delete query.focus
    router.replace({ query: { ...query, tab } })
  })

  return {
    activeDetailTab,
    activeDevWorkspaceTab,
    detailTabs,
    loadFailed,
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
    loadTemplate,
    resetTransientDetailState,
    syncTabFromRoute,
    backToList,
    openLifecyclePanel,
    openDevWorkspaceTab,
    openTestPreviewTab,
    scrollToLifecyclePanel,
    scrollToDevWorkspace,
  }
}
