import { computed, nextTick, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { canViewCollaborationWorkItems } from '@/auth/roles'
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
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
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
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'
import type { PreviewRecord } from '@/types/template'
import {
  useTemplateLifecycleActions,
  type GovernanceAction,
  type LifecycleDecisionDialogMode,
} from '@/views/templates/useTemplateLifecycleActions'
import { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'

export type { GovernanceAction, LifecycleDecisionDialogMode }

export function useTemplateDetailController(workspace: Ref<'legacy' | 'dev-editor'>) {
  const isDevEditor = computed(() => workspace.value === 'dev-editor')

  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
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
    exportTemplates,
    editTemplateMetadata,
    context,
  } = useCapabilities()

  const lastPreview = ref<PreviewRecord | null>(null)
  const selectedPreviewId = ref<string | null>(null)
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
  const selectedTestDataSetId = ref<string | null>(null)
  const generatingPreview = ref(false)
  const generatingPreviewId = ref<string | null>(null)
  const batchTesting = ref(false)
  const coverageRefreshToken = ref(0)
  const metadataEditOpen = ref(false)
  const loadFailed = ref(false)
  const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)

  const activeDetailTab = ref<TemplateDetailTab>(
    isDevEditor.value ? 'authoring' : resolveTemplateDetailTabFromQuery(route.query),
  )
  const activeDevWorkspaceTab = ref<TemplateDevWorkspaceTab>(
    resolveTemplateDevWorkspaceTabFromQuery(route.query),
  )

  const templateId = computed(() => route.params.templateId as string)
  const devVersionId = computed(() =>
    isDevEditor.value ? String(route.params.devVersionId ?? '') : '',
  )

  const loadTemplateHolder = { fn: async (): Promise<void> => {} }

  const templateMatchesRoute = computed(
    () => templatesStore.selectedTemplate?.id === templateId.value,
  )

  const template = computed(() => {
    if (!templateMatchesRoute.value) {
      return null
    }
    return templatesStore.selectedTemplate
  })

  const showDetailSkeleton = computed(() => {
    if (templatesStore.loadingDetail) {
      return true
    }
    const selected = templatesStore.selectedTemplate
    return selected !== null && selected.id !== templateId.value
  })

  const errorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.error.loadDetail')
  })

  const lifecycle = useTemplateLifecycleActions({
    templateId,
    template,
    isDevEditor,
    errorMessage,
    loadTemplate: () => loadTemplateHolder.fn(),
    activeDetailTab,
  })

  const policy = useTemplatePolicyCredentials({
    templateId,
    template,
    errorMessage,
  })

  const showMetadataEdit = computed(() => {
    const status = template.value?.lifecycleStatus
    if (!status || !editTemplateMetadata.value) {
      return false
    }
    return status !== 'PUBLISHED' && status !== 'STOPPED' && status !== 'DEPRECATED'
  })
  const showExportActions = computed(
    () =>
      exportTemplates.value &&
      Boolean(template.value) &&
      isTemplateExportEligible(template.value!.lifecycleStatus),
  )

  const showAuthoringSection = computed(() => {
    const status = template.value?.lifecycleStatus
    if (
      !status ||
      status === 'PUBLISHED' ||
      status === 'STOPPED' ||
      status === 'DEPRECATED'
    ) {
      return false
    }
    if (authorTemplates.value) {
      return true
    }
    return (
      isDevEditor.value &&
      decideTests.value &&
      (status === 'DRAFT' || status === 'TESTING')
    )
  })
  const canEditContentModuleReferences = computed(
    () => authorTemplates.value && template.value?.lifecycleStatus === 'DRAFT',
  )
  const showTestGenerate = computed(
    () =>
      authorTemplates.value &&
      (template.value?.lifecycleStatus === 'DRAFT' ||
        template.value?.lifecycleStatus === 'TESTING'),
  )

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
    if (!template.value || !shouldShowTemplateAuthorJourney({
      authorTemplates: authorTemplates.value,
      roles: sessionStore.session?.roles ?? [],
    })) {
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

  function backToList() {
    if (isDevEditor.value) {
      router.push(templatePackageHubPath(templateId.value))
      return
    }
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  }

  async function handleTestGenerate(testDataSetId?: string) {
    const resolvedId = testDataSetId ?? selectedTestDataSetId.value ?? undefined
    generatingPreview.value = true
    generatingPreviewId.value = resolvedId ?? null
    try {
      const preview = await templatesStore.testGenerate(templateId.value, {
        testDataSetId: resolvedId,
      })
      lastPreview.value = preview
      selectedPreviewId.value = preview.previewId
      if (resolvedId) {
        selectedTestDataSetId.value = resolvedId
      }
      openDevWorkspaceTab('testing')
      ElMessage.success(t('templates.testGenerate.success', { previewId: preview.previewId }))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.testGenerate'))
    } finally {
      generatingPreview.value = false
      generatingPreviewId.value = null
    }
  }

  async function handleBatchTestGenerate() {
    batchTesting.value = true
    try {
      const dataSets = await templatesApi.listTestDataSets(templateId.value)
      if (dataSets.length === 0) {
        ElMessage.warning(t('templates.testDataSets.error.noDataSetsForBatch'))
        return
      }
      const summary = await templatesApi.batchTestGenerate(templateId.value, {
        testDataSetIds: dataSets.map((row) => row.testDataSetId),
      })
      coverageRefreshToken.value += 1
      openDevWorkspaceTab('testing')
      ElMessage.success(
        t('templates.testDataSets.batchSuccess', {
          succeeded: summary.succeededCount,
          failed: summary.failedCount,
          warnings: summary.warningCount,
        }),
      )
    } catch {
      ElMessage.error(t('templates.testDataSets.error.batch'))
    } finally {
      batchTesting.value = false
    }
  }

  function openTestPreviewTab() {
    openDevWorkspaceTab('testing')
  }

  async function handlePreviewSelected(previewId: string | null) {
    selectedPreviewId.value = previewId
    if (!previewId) {
      lastPreview.value = null
      return
    }
    try {
      lastPreview.value = await templatesApi.getPreview(templateId.value, previewId)
    } catch {
      ElMessage.error(errorMessage.value || t('templates.previewHistory.error.loadDetail'))
    }
  }

  async function handleMetadataUpdate(payload: { name: string; description: string | null }) {
    try {
      await templatesStore.updateTemplateMetadata(templateId.value, payload)
      metadataEditOpen.value = false
      ElMessage.success(t('templates.metadata.success'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.updateMetadata'))
    }
  }

  return {
    // i18n
    t,
    te,
    // formatters
    formatDateTime,
    // route state
    templateId,
    devVersionId,
    // core mode
    isDevEditor,
    // template data
    template,
    showDetailSkeleton,
    loadFailed,
    // tabs
    activeDetailTab,
    activeDevWorkspaceTab,
    detailTabs,
    // capabilities
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    // journey display
    showAuthorJourney,
    authorJourneyContext,
    showTesterJourney,
    testerJourneyContext,
    showApproverJourney,
    approverJourneyContext,
    showTeamLeadJourney,
    teamLeadJourneyContext,
    authorJourneyPrimaryCtaDisabled: lifecycle.authorJourneyPrimaryCtaDisabled,
    // journey handlers
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
    // lifecycle visibility
    showLifecycleSection: lifecycle.showLifecycleSection,
    showGovernanceSection: lifecycle.showGovernanceSection,
    showDraftActions: lifecycle.showDraftActions,
    showTestingDecisionActions: lifecycle.showTestingDecisionActions,
    showSubmitForApproval: lifecycle.showSubmitForApproval,
    showApprovalDecisionActions: lifecycle.showApprovalDecisionActions,
    showPublishActions: lifecycle.showPublishActions,
    showTestGenerate,
    showStopAction: lifecycle.showStopAction,
    showRestoreAction: lifecycle.showRestoreAction,
    showDeprecateAction: lifecycle.showDeprecateAction,
    // authoring visibility
    showAuthoringSection,
    canEditContentModuleReferences,
    // policy
    showPolicyPanel: policy.showPolicyPanel,
    canPolicy: policy.canPolicy,
    showExportActions,
    showDeleteTemplateAction: lifecycle.showDeleteTemplateAction,
    showMetadataEdit,
    policyLoadFailed: policy.policyLoadFailed,
    // publish gate
    publishGateItems: lifecycle.publishGateItems,
    publishGateReady: lifecycle.publishGateReady,
    publishVersion: lifecycle.publishVersion,
    publishBumpLevel: lifecycle.publishBumpLevel,
    publishBumpOptions: lifecycle.publishBumpOptions,
    publishVersionConflict: lifecycle.publishVersionConflict,
    loadingPublishGate: lifecycle.loadingPublishGate,
    publishGateLoadError: lifecycle.publishGateLoadError,
    publishCoverageSummary: lifecycle.publishCoverageSummary,
    publishChangeDiffSummary: lifecycle.publishChangeDiffSummary,
    // submit gate
    submitGateItems: lifecycle.submitGateItems,
    submitGateReady: lifecycle.submitGateReady,
    loadingSubmitGate: lifecycle.loadingSubmitGate,
    submitGateLoadError: lifecycle.submitGateLoadError,
    submitCoverageSummary: lifecycle.submitCoverageSummary,
    submitChangeDiffSummary: lifecycle.submitChangeDiffSummary,
    // binding gate
    bindingGateResult: lifecycle.bindingGateResult,
    // dialog state
    lifecycleComment: lifecycle.lifecycleComment,
    lifecycleCommentDialogOpen: lifecycle.lifecycleCommentDialogOpen,
    decisionDialogOpen: lifecycle.decisionDialogOpen,
    decisionDialogMode: lifecycle.decisionDialogMode,
    publishSummaryOpen: lifecycle.publishSummaryOpen,
    submitSummaryOpen: lifecycle.submitSummaryOpen,
    metadataEditOpen,
    credentialSecretDialogVisible: policy.credentialSecretDialogVisible,
    credentialSecretValue: policy.credentialSecretValue,
    credentialSecretExternalId: policy.credentialSecretExternalId,
    displayedCredentialSecret: policy.displayedCredentialSecret,
    // test state
    lastPreview,
    selectedPreviewId,
    selectedTestDataSetId,
    generatingPreview,
    generatingPreviewId,
    batchTesting,
    coverageRefreshToken,
    submitForTestDialogOpen: lifecycle.submitForTestDialogOpen,
    // tester evidence
    testerEvidenceViewed,
    approverEvidenceViewed,
    // credentials table
    credentialColumnFilters: policy.credentialColumnFilters,
    credentialsCurrentPage: policy.credentialsCurrentPage,
    paginatedCredentials: policy.paginatedCredentials,
    credentialStatusFilterOptions: policy.credentialStatusFilterOptions,
    totalCredentialRows: policy.totalCredentialRows,
    sortCredentialsByCreatedAt: policy.sortCredentialsByCreatedAt,
    // contract
    selectedContractEnvironment,
    // store
    templatesStore,
    // handlers
    loadTemplate,
    loadPublishGateData: lifecycle.loadPublishGateData,
    loadSubmitGateData: lifecycle.loadSubmitGateData,
    loadPolicyData: policy.loadPolicyData,
    backToList,
    openLifecyclePanel,
    openApiPolicyConsole: policy.openApiPolicyConsole,
    openDevWorkspaceTab,
    handleTestGenerate,
    handleBatchTestGenerate,
    handlePreviewSelected,
    handleSubmitForTest: lifecycle.handleSubmitForTest,
    handleTestDecision: lifecycle.handleTestDecision,
    openApprovalRejectDialog: lifecycle.openApprovalRejectDialog,
    submitLifecycleDecision: lifecycle.submitLifecycleDecision,
    handleSubmitForApproval: lifecycle.handleSubmitForApproval,
    confirmSubmitFromSummary: lifecycle.confirmSubmitFromSummary,
    handleApprovalDecision: lifecycle.handleApprovalDecision,
    handlePublish: lifecycle.handlePublish,
    confirmPublishFromSummary: lifecycle.confirmPublishFromSummary,
    handleGovernanceAction: lifecycle.handleGovernanceAction,
    handleMetadataUpdate,
    handleCreateCredential: policy.handleCreateCredential,
    handleRotateCredential: policy.handleRotateCredential,
    handleRevokeCredential: policy.handleRevokeCredential,
    handleDeleteTemplate: lifecycle.handleDeleteTemplate,
  }
}
