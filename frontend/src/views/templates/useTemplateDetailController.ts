import { computed, nextTick, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useCredentialStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, apiPolicyDetailPath, templatePackageHubPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import { conflictsWithExisting, suggestNextVersions, type SemverBumpLevel } from '@/utils/semver'
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
import { resolveWorkflowBannerActionKind } from '@/utils/templateWorkflowBannerContext'
import {
  isPublishGateReady,
  isSubmitGateReady,
  mapPublishGateChecklistItems,
} from '@/utils/templateLifecycleDecisionForm'
import { resolvePublishGateLoadErrorKey } from '@/utils/templateBindingGateDisplay'
import type {
  ApiCredentialSummary,
  BindingValidationResult,
  ChangeDiffSummary,
  CoverageSummary,
  DeleteTemplatePayload,
  LifecycleGovernanceAction,
  PreviewRecord,
  PublishGateChecklist,
} from '@/types/template'

export type GovernanceAction = 'stop' | 'restore' | 'deprecate'

export type LifecycleDecisionDialogMode = 'test-fail' | 'test-pass' | 'approval-reject' | 'approval-approve'

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
    stopTemplates,
    restoreOrDeprecateTemplates,
    manageApiPolicy,
    deleteTemplates,
    exportTemplates,
    editTemplateMetadata,
    context,
  } = useCapabilities()
  const { confirmAction } = useConfirmAction()

  const lifecycleComment = ref('')
  const decisionDialogOpen = ref(false)
  const decisionDialogMode = ref<LifecycleDecisionDialogMode>('test-fail')
  const publishBumpLevel = ref<SemverBumpLevel>('patch')
  const publishVersion = ref('1.0.0')
  const publishSummaryOpen = ref(false)
  const submitSummaryOpen = ref(false)
  const publishedReleaseVersions = ref<string[]>([])
  const credentialSecretDialogVisible = ref(false)
  const credentialSecretValue = ref('')
  const credentialSecretExternalId = ref('')
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
  const submitForTestDialogOpen = ref(false)
  const lifecycleCommentDialogOpen = ref(false)
  const bindingGateResult = ref<BindingValidationResult | null>(null)
  const publishGateChecklist = ref<PublishGateChecklist | null>(null)
  const submitGateChecklist = ref<PublishGateChecklist | null>(null)
  const publishCoverageSummary = ref<CoverageSummary | null>(null)
  const submitCoverageSummary = ref<CoverageSummary | null>(null)
  const publishChangeDiffSummary = ref<ChangeDiffSummary | null>(null)
  const submitChangeDiffSummary = ref<ChangeDiffSummary | null>(null)
  const loadingPublishGate = ref(false)
  const loadingSubmitGate = ref(false)
  const publishGateLoadError = ref<string | null>(null)
  const submitGateLoadError = ref<string | null>(null)
  const policyLoadFailed = ref(false)
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

  const credentialsSource = computed(() => templatesStore.credentials)
  const { filters: credentialColumnFilters, filteredRows: filteredCredentials } = useDataTableFilters(
    credentialsSource,
    [
      { key: 'externalId', getValue: (row) => row.externalId },
      { key: 'status', getValue: (row) => row.status, matchMode: 'exact' },
      { key: 'createdAt', getValue: (row) => formatDateTime(row.createdAt) },
    ],
  )
  const credentialsCurrentPage = ref(1)
  const { paginatedRows: paginatedCredentials, totalRows: totalCredentialRows } = useCatalogPagination(
    filteredCredentials,
    credentialsCurrentPage,
    CLIENT_TABLE_PAGE_SIZE,
  )
  const sortCredentialsByCreatedAt = rowSortMethod<ApiCredentialSummary>((row) => row.createdAt)
  const credentialStatusFilterOptions = useCredentialStatusFilterOptions()

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

  const canPolicy = computed(() => manageApiPolicy.value)

  const errorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.error.loadDetail')
  })

  const approvalSubState = computed(() => template.value?.approvalSubState)

  const workflowBannerCapabilities = computed(() => ({
    authorTemplates: authorTemplates.value,
    decideTests: decideTests.value,
    decideApprovals: decideApprovals.value,
    publishTemplates: publishTemplates.value,
  }))

  const workflowBannerActionKind = computed(() => {
    const status = template.value?.lifecycleStatus
    if (!status) {
      return null
    }
    return resolveWorkflowBannerActionKind(
      status,
      workflowBannerCapabilities.value,
      template.value?.approvalSubState ?? null,
    )
  })

  const showDraftActions = computed(() => workflowBannerActionKind.value === 'draft')
  const showTestingDecisionActions = computed(() => workflowBannerActionKind.value === 'testing')
  const showSubmitForApproval = computed(() => {
    if (template.value?.lifecycleStatus !== 'APPROVAL' || !authorTemplates.value) {
      return false
    }
    if (approvalSubState.value === 'PENDING_DECISION') {
      return false
    }
    if (decideApprovals.value && !authorTemplates.value) {
      return false
    }
    return true
  })
  const showApprovalDecisionActions = computed(() => {
    if (template.value?.lifecycleStatus !== 'APPROVAL' || !decideApprovals.value) {
      return false
    }
    if (approvalSubState.value === 'PENDING_SUBMIT') {
      return false
    }
    return true
  })
  const showPublishActions = computed(() => workflowBannerActionKind.value === 'publish')
  const showStopAction = computed(
    () => template.value?.lifecycleStatus === 'PUBLISHED' && stopTemplates.value,
  )
  const showRestoreAction = computed(
    () => template.value?.lifecycleStatus === 'STOPPED' && restoreOrDeprecateTemplates.value,
  )
  const showDeprecateAction = computed(
    () => template.value?.lifecycleStatus === 'STOPPED' && restoreOrDeprecateTemplates.value,
  )
  const showGovernanceSection = computed(
    () => showStopAction.value || showRestoreAction.value || showDeprecateAction.value,
  )
  const showMetadataEdit = computed(() => {
    const status = template.value?.lifecycleStatus
    if (!status || !editTemplateMetadata.value) {
      return false
    }
    return status !== 'PUBLISHED' && status !== 'STOPPED' && status !== 'DEPRECATED'
  })
  const showDeleteTemplateAction = computed(
    () => deleteTemplates.value && template.value?.lifecycleStatus !== 'DELETED',
  )
  const showExportActions = computed(
    () =>
      exportTemplates.value &&
      Boolean(template.value) &&
      isTemplateExportEligible(template.value!.lifecycleStatus),
  )

  function resolvePublishGateItemLabel(item: {
    checkCode: string
    messageKey: string
    summary: string
  }): string {
    if (te(item.messageKey)) {
      return t(item.messageKey)
    }
    const codeKey = `templates.publishGate.checkCodes.${item.checkCode}`
    if (te(codeKey)) {
      return t(codeKey)
    }
    return item.summary
  }

  const publishGateItems = computed(() => {
    const apiItems = publishGateChecklist.value
      ? mapPublishGateChecklistItems(publishGateChecklist.value.items, resolvePublishGateItemLabel)
      : []
    return [
      {
        key: 'releaseVersion',
        label: t('templates.publishGate.releaseVersionProvided'),
        ready: Boolean(publishVersion.value.trim()),
        informational: false,
      },
      ...apiItems,
    ]
  })

  const publishGateReady = computed(() =>
    isPublishGateReady({
      checklistReady: Boolean(publishGateChecklist.value?.ready),
      releaseVersion: publishVersion.value,
      versionConflict: publishVersionConflict.value,
    }),
  )

  const submitGateItems = computed(() => {
    if (!submitGateChecklist.value) {
      return []
    }
    return mapPublishGateChecklistItems(
      submitGateChecklist.value.items,
      resolvePublishGateItemLabel,
    )
  })

  const submitGateReady = computed(() =>
    isSubmitGateReady({
      checklistReady: Boolean(submitGateChecklist.value?.ready),
    }),
  )

  const authorJourneyPrimaryCtaDisabled = computed(
    () =>
      showSubmitForApproval.value &&
      (loadingSubmitGate.value || !submitGateReady.value || Boolean(submitGateLoadError.value)),
  )

  const suggestedVersions = computed(() =>
    suggestNextVersions(template.value?.releaseVersion ?? null),
  )

  const publishVersionConflict = computed(() =>
    conflictsWithExisting(publishVersion.value, publishedReleaseVersions.value),
  )

  const publishBumpOptions = computed(() => [
    {
      level: 'major' as SemverBumpLevel,
      label: t('templates.lifecycle.bumpMajor'),
      version: suggestedVersions.value.major,
    },
    {
      level: 'minor' as SemverBumpLevel,
      label: t('templates.lifecycle.bumpMinor'),
      version: suggestedVersions.value.minor,
    },
    {
      level: 'patch' as SemverBumpLevel,
      label: t('templates.lifecycle.bumpPatch'),
      version: suggestedVersions.value.patch,
    },
  ])

  const showPolicyPanel = computed(
    () => template.value?.lifecycleStatus === 'PUBLISHED' && canPolicy.value,
  )
  const showLifecycleSection = computed(
    () =>
      showDraftActions.value ||
      showTestingDecisionActions.value ||
      showSubmitForApproval.value ||
      showApprovalDecisionActions.value ||
      showPublishActions.value ||
      (authorTemplates.value &&
        (template.value?.lifecycleStatus === 'DRAFT' ||
          template.value?.lifecycleStatus === 'TESTING')),
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

  const displayedCredentialSecret = computed(() => {
    if (templatesStore.lastCreatedCredential?.secret) {
      return templatesStore.lastCreatedCredential.secret
    }
    return templatesStore.lastRotatedCredential?.secret ?? ''
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
      publishGateReady: publishGateReady.value,
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
    if (showPolicyPanel.value) {
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
    submitForTestDialogOpen.value = true
  }

  async function handleJourneySubmitForApproval() {
    openLifecyclePanel()
    await handleSubmitForApproval()
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
    await loadPublishGateData()
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
    await handlePublish()
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
    lifecycleComment.value = ''
    lastPreview.value = null
    bindingGateResult.value = null
    publishGateChecklist.value = null
    submitGateChecklist.value = null
    publishCoverageSummary.value = null
    submitCoverageSummary.value = null
    publishChangeDiffSummary.value = null
    submitChangeDiffSummary.value = null
    publishedReleaseVersions.value = []
    loadingPublishGate.value = false
    loadingSubmitGate.value = false
    publishGateLoadError.value = null
    submitGateLoadError.value = null
    policyLoadFailed.value = false
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

  watch(
    showPublishActions,
    async (active) => {
      if (!active) {
        bindingGateResult.value = null
        publishGateChecklist.value = null
        publishCoverageSummary.value = null
        publishChangeDiffSummary.value = null
        publishedReleaseVersions.value = []
        publishGateLoadError.value = null
        return
      }
      publishBumpLevel.value = 'patch'
      publishVersion.value = suggestedVersions.value.patch
      await loadPublishGateData()
    },
    { immediate: true },
  )

  watch(
    showSubmitForApproval,
    async (active) => {
      if (!active) {
        submitGateChecklist.value = null
        submitCoverageSummary.value = null
        submitChangeDiffSummary.value = null
        submitGateLoadError.value = null
        return
      }
      await loadSubmitGateData()
    },
    { immediate: true },
  )

  watch(publishBumpLevel, (level) => {
    publishVersion.value = suggestedVersions.value[level]
  })

  watch(suggestedVersions, (versions) => {
    publishVersion.value = versions[publishBumpLevel.value]
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
      if (showPolicyPanel.value) {
        await loadPolicyData()
      }
    } catch {
      loadFailed.value = true
    }
  }

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

  async function loadSubmitGateData() {
    submitGateLoadError.value = null
    loadingSubmitGate.value = true
    try {
      const [checklist, coverage, changeDiff] = await Promise.all([
        templatesApi.fetchPublishGate(templateId.value, 'SUBMIT_FOR_APPROVAL'),
        templatesApi.getTemplateCoverage(templateId.value),
        templatesApi.fetchChangeDiff(templateId.value),
      ])
      submitGateChecklist.value = checklist
      submitCoverageSummary.value = coverage
      submitChangeDiffSummary.value = changeDiff
    } catch {
      submitGateLoadError.value = resolvePublishGateLoadErrorKey(templatesStore.lastErrorMessageKey)
      submitGateChecklist.value = null
      submitCoverageSummary.value = null
      submitChangeDiffSummary.value = null
    } finally {
      loadingSubmitGate.value = false
    }
  }

  async function loadPublishGateData() {
    publishGateLoadError.value = null
    loadingPublishGate.value = true
    try {
      await templatesStore.fetchApiPolicy(templateId.value)
      const [bindings, checklist, coverage, changeDiff, versions] = await Promise.all([
        templatesStore.validateBindings(templateId.value),
        templatesApi.fetchPublishGate(templateId.value),
        templatesApi.getTemplateCoverage(templateId.value),
        templatesApi.fetchChangeDiff(templateId.value),
        templatesApi.fetchReleaseVersions(templateId.value),
      ])
      bindingGateResult.value = bindings
      publishGateChecklist.value = checklist
      publishCoverageSummary.value = coverage
      publishChangeDiffSummary.value = changeDiff
      publishedReleaseVersions.value = versions.map((entry) => entry.releaseVersion)
    } catch {
      publishGateLoadError.value = resolvePublishGateLoadErrorKey(templatesStore.lastErrorMessageKey)
      bindingGateResult.value = null
      publishGateChecklist.value = null
      publishCoverageSummary.value = null
      publishChangeDiffSummary.value = null
      publishedReleaseVersions.value = []
    } finally {
      loadingPublishGate.value = false
    }
  }

  async function loadPolicyData() {
    policyLoadFailed.value = false
    try {
      await Promise.all([
        templatesStore.fetchApiPolicy(templateId.value),
        templatesStore.fetchCredentials(templateId.value),
      ])
    } catch {
      policyLoadFailed.value = true
    }
  }

  function openApiPolicyConsole() {
    router.push(apiPolicyDetailPath(templateId.value))
  }

  function backToList() {
    if (isDevEditor.value) {
      router.push(templatePackageHubPath(templateId.value))
      return
    }
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  }

  function openCredentialSecretDialog(externalId: string, secret: string) {
    credentialSecretExternalId.value = externalId
    credentialSecretValue.value = secret
    credentialSecretDialogVisible.value = true
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

  async function handleSubmitForTest(comment = '') {
    try {
      await templatesStore.submitForTest(templateId.value, { commentSummary: comment })
      lifecycleComment.value = ''
      ElMessage.success(t('templates.lifecycle.submitTestSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleTestDecision(decision: 'PASSED' | 'FAILED') {
    if (decision === 'FAILED') {
      decisionDialogMode.value = 'test-fail'
      decisionDialogOpen.value = true
      return
    }
    decisionDialogMode.value = 'test-pass'
    decisionDialogOpen.value = true
  }

  function openApprovalRejectDialog() {
    decisionDialogMode.value = 'approval-reject'
    decisionDialogOpen.value = true
  }

  async function submitLifecycleDecision(payload: {
    reasonCategory?: string
    impactSummary?: string
    commentSummary?: string
    remediationTestRecordId?: string
    remediationChangeDiffRef?: string
    remediationChecklistCode?: string
    fidelityViewedConfirmed?: boolean
    coverageViewedConfirmed?: boolean
    previewViewedConfirmed?: boolean
    exceptionIntervention?: boolean
    exceptionReason?: string
    secondaryConfirmed?: boolean
    keyEvidenceConfirmed?: boolean
  }) {
    const mode = decisionDialogMode.value
    try {
      if (mode === 'test-fail') {
        await templatesStore.recordTestDecision(templateId.value, {
          decision: 'FAILED',
          reasonCategory: payload.reasonCategory,
          impactSummary: payload.impactSummary,
          commentSummary: payload.commentSummary,
          remediationTestRecordId: payload.remediationTestRecordId,
          remediationChangeDiffRef: payload.remediationChangeDiffRef,
          remediationChecklistCode: payload.remediationChecklistCode,
        })
        ElMessage.success(t('templates.lifecycle.testDecisionSuccess'))
      } else if (mode === 'test-pass') {
        await templatesStore.recordTestDecision(templateId.value, {
          decision: 'PASSED',
          commentSummary: payload.commentSummary,
          fidelityViewedConfirmed: payload.fidelityViewedConfirmed,
          coverageViewedConfirmed: payload.coverageViewedConfirmed,
          previewViewedConfirmed: payload.previewViewedConfirmed,
          exceptionIntervention: payload.exceptionIntervention,
          exceptionReason: payload.exceptionReason,
          secondaryConfirmed: payload.secondaryConfirmed,
        })
        ElMessage.success(t('templates.lifecycle.testDecisionSuccess'))
      } else if (mode === 'approval-reject') {
        await templatesStore.recordApprovalDecision(templateId.value, {
          decision: 'REJECTED',
          reasonCategory: payload.reasonCategory,
          impactSummary: payload.impactSummary,
          commentSummary: payload.commentSummary,
          remediationTestRecordId: payload.remediationTestRecordId,
          remediationChangeDiffRef: payload.remediationChangeDiffRef,
          remediationChecklistCode: payload.remediationChecklistCode,
        })
        ElMessage.success(t('templates.lifecycle.approvalDecisionSuccess'))
      } else if (mode === 'approval-approve') {
        await templatesStore.recordApprovalDecision(templateId.value, {
          decision: 'APPROVED',
          commentSummary: payload.commentSummary,
          keyEvidenceConfirmed: payload.keyEvidenceConfirmed,
          exceptionIntervention: payload.exceptionIntervention,
          exceptionReason: payload.exceptionReason,
          secondaryConfirmed: payload.secondaryConfirmed,
        })
        ElMessage.success(t('templates.lifecycle.approvalDecisionSuccess'))
      }
      decisionDialogOpen.value = false
      lifecycleComment.value = ''
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleSubmitForApproval() {
    if (submitGateLoadError.value) {
      ElMessage.error(t('templates.submitGate.loadError'))
      return
    }
    if (loadingSubmitGate.value) {
      return
    }
    if (!submitGateReady.value) {
      ElMessage.warning(t('templates.lifecycle.submitGateBlocked'))
      return
    }
    submitSummaryOpen.value = true
  }

  async function confirmSubmitFromSummary() {
    submitSummaryOpen.value = false
    try {
      await templatesStore.submitForApproval(templateId.value, {
        commentSummary: lifecycleComment.value,
      })
      lifecycleComment.value = ''
      ElMessage.success(t('templates.lifecycle.submitApprovalSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleApprovalDecision(decision: 'APPROVED' | 'REJECTED') {
    if (decision === 'REJECTED') {
      openApprovalRejectDialog()
      return
    }
    decisionDialogMode.value = 'approval-approve'
    decisionDialogOpen.value = true
  }

  async function handlePublish() {
    if (!publishGateReady.value) {
      return
    }
    publishSummaryOpen.value = true
  }

  async function confirmPublishFromSummary() {
    publishSummaryOpen.value = false
    try {
      await templatesStore.publishTemplate(templateId.value, {
        releaseVersion: publishVersion.value,
      })
      await loadTemplate()
      if (isDevEditor.value) {
        router.push(templatePackageHubPath(templateId.value))
      } else {
        activeDetailTab.value = 'releaseVersions'
      }
      ElMessage.success(t('templates.lifecycle.publishSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  const governanceActionConfig = {
    stop: {
      previewAction: 'STOP' as LifecycleGovernanceAction,
      titleKey: 'templates.lifecycle.stopTitle',
      reasonKey: 'templates.lifecycle.stopReasonPrompt',
      confirmTitleKey: 'templates.lifecycle.confirmStopTitle',
      confirmMessageKey: 'templates.lifecycle.confirmStopMessage',
      successKey: 'templates.lifecycle.stopSuccess',
    },
    restore: {
      previewAction: 'RESTORE' as LifecycleGovernanceAction,
      titleKey: 'templates.lifecycle.restoreTitle',
      reasonKey: 'templates.lifecycle.restoreReasonPrompt',
      confirmTitleKey: 'templates.lifecycle.confirmRestoreTitle',
      confirmMessageKey: 'templates.lifecycle.confirmRestoreMessage',
      successKey: 'templates.lifecycle.restoreSuccess',
    },
    deprecate: {
      previewAction: 'DEPRECATE' as LifecycleGovernanceAction,
      titleKey: 'templates.lifecycle.deprecateTitle',
      reasonKey: 'templates.lifecycle.deprecateReasonPrompt',
      confirmTitleKey: 'templates.lifecycle.confirmDeprecateTitle',
      confirmMessageKey: 'templates.lifecycle.confirmDeprecateMessage',
      successKey: 'templates.lifecycle.deprecateSuccess',
    },
  } as const

  async function buildImpactPreviewMessage(
    action: LifecycleGovernanceAction,
    releaseVersion?: string,
  ): Promise<string> {
    const preview = await templatesStore.fetchLifecycleImpactPreview(templateId.value, {
      action,
      releaseVersion,
    })
    const summary = te(preview.summaryMessageKey)
      ? t(preview.summaryMessageKey)
      : t(`templates.governance.impactSummary.${action}`)
    const callable = preview.callableReleaseVersions.length
      ? t('templates.governance.impactCallableVersions', {
          versions: preview.callableReleaseVersions.join(', '),
        })
      : t('templates.governance.impactNoCallableVersions')
    const defaultRoute = preview.defaultRouteReleaseVersion
      ? t('templates.governance.impactDefaultRoute', {
          version: preview.defaultRouteReleaseVersion,
        })
      : ''
    const routeImpact = preview.defaultRouteImpacted
      ? t('templates.governance.impactDefaultRouteAffected')
      : ''
    return [summary, callable, defaultRoute, routeImpact, t('templates.governance.impactConfirmPrompt')]
      .filter(Boolean)
      .join('\n\n')
  }

  async function handleGovernanceAction(action: GovernanceAction) {
    const config = governanceActionConfig[action]
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(t(config.reasonKey), t(config.titleKey), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValidator: (value) =>
          value.trim().length > 0 ? true : t('templates.lifecycle.reasonRequired'),
      })
      reason = result.value.trim()
    } catch {
      return
    }

    try {
      const impactMessage = await buildImpactPreviewMessage(config.previewAction)
      const confirmBody = [impactMessage, t(config.confirmMessageKey)].join('\n\n')
      await ElMessageBox.confirm(confirmBody, t(config.confirmTitleKey), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      })
    } catch {
      return
    }

    const payload = { reason, confirmed: true }
    try {
      if (action === 'stop') {
        await templatesStore.stopTemplate(templateId.value, payload)
      } else if (action === 'restore') {
        await templatesStore.restoreTemplate(templateId.value, payload)
      } else {
        await templatesStore.deprecateTemplate(templateId.value, payload)
      }
      ElMessage.success(t(config.successKey))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
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

  async function handleCreateCredential() {
    try {
      const created = await templatesStore.createCredential(templateId.value)
      openCredentialSecretDialog(created.externalId, created.secret)
      ElMessage.success(t('templates.policy.createCredentialSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.createCredential'))
    }
  }

  async function handleRotateCredential(credentialId: string, externalId: string) {
    const confirmed = await confirmAction({
      titleKey: 'templates.policy.confirmRotateTitle',
      messageKey: 'templates.policy.confirmRotateMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    try {
      const rotated = await templatesStore.rotateCredential(templateId.value, credentialId)
      openCredentialSecretDialog(externalId, rotated.secret)
      ElMessage.success(t('templates.policy.rotateCredentialSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.rotateCredential'))
    }
  }

  async function handleRevokeCredential(credentialId: string) {
    const confirmed = await confirmAction({
      titleKey: 'templates.policy.confirmRevokeTitle',
      messageKey: 'templates.policy.confirmRevokeMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    try {
      await templatesStore.revokeCredential(templateId.value, credentialId)
      ElMessage.success(t('templates.policy.revokeCredentialSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.revokeCredential'))
    }
  }

  async function handleDeleteTemplate() {
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(
        t('templates.deleteAction.reasonPrompt'),
        t('templates.deleteAction.title'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputValidator: (value) =>
            value.trim().length > 0 ? true : t('templates.deleteAction.reasonRequired'),
        },
      )
      reason = result.value.trim()
    } catch {
      return
    }

    const confirmed = await confirmAction({
      titleKey: 'templates.deleteAction.confirmTitle',
      messageKey: 'templates.deleteAction.confirmMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }

    try {
      const payload: DeleteTemplatePayload = { reason }
      await templatesStore.deleteTemplate(templateId.value, payload)
      ElMessage.success(t('templates.deleteAction.success'))
      router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.delete'))
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
    authorJourneyPrimaryCtaDisabled,
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
    showLifecycleSection,
    showGovernanceSection,
    showDraftActions,
    showTestingDecisionActions,
    showSubmitForApproval,
    showApprovalDecisionActions,
    showPublishActions,
    showTestGenerate,
    showStopAction,
    showRestoreAction,
    showDeprecateAction,
    // authoring visibility
    showAuthoringSection,
    canEditContentModuleReferences,
    // policy
    showPolicyPanel,
    canPolicy,
    showExportActions,
    showDeleteTemplateAction,
    showMetadataEdit,
    policyLoadFailed,
    // publish gate
    publishGateItems,
    publishGateReady,
    publishVersion,
    publishBumpLevel,
    publishBumpOptions,
    publishVersionConflict,
    loadingPublishGate,
    publishGateLoadError,
    publishCoverageSummary,
    publishChangeDiffSummary,
    // submit gate
    submitGateItems,
    submitGateReady,
    loadingSubmitGate,
    submitGateLoadError,
    submitCoverageSummary,
    submitChangeDiffSummary,
    // binding gate
    bindingGateResult,
    // dialog state
    lifecycleComment,
    lifecycleCommentDialogOpen,
    decisionDialogOpen,
    decisionDialogMode,
    publishSummaryOpen,
    submitSummaryOpen,
    metadataEditOpen,
    credentialSecretDialogVisible,
    credentialSecretValue,
    credentialSecretExternalId,
    displayedCredentialSecret,
    // test state
    lastPreview,
    selectedPreviewId,
    selectedTestDataSetId,
    generatingPreview,
    generatingPreviewId,
    batchTesting,
    coverageRefreshToken,
    submitForTestDialogOpen,
    // tester evidence
    testerEvidenceViewed,
    approverEvidenceViewed,
    // credentials table
    credentialColumnFilters,
    credentialsCurrentPage,
    paginatedCredentials,
    credentialStatusFilterOptions,
    totalCredentialRows,
    sortCredentialsByCreatedAt,
    // contract
    selectedContractEnvironment,
    // store
    templatesStore,
    // handlers
    loadTemplate,
    loadPublishGateData,
    loadSubmitGateData,
    loadPolicyData,
    backToList,
    openLifecyclePanel,
    openApiPolicyConsole,
    openDevWorkspaceTab,
    handleTestGenerate,
    handleBatchTestGenerate,
    handlePreviewSelected,
    handleSubmitForTest,
    handleTestDecision,
    openApprovalRejectDialog,
    submitLifecycleDecision,
    handleSubmitForApproval,
    confirmSubmitFromSummary,
    handleApprovalDecision,
    handlePublish,
    confirmPublishFromSummary,
    handleGovernanceAction,
    handleMetadataUpdate,
    handleCreateCredential,
    handleRotateCredential,
    handleRevokeCredential,
    handleDeleteTemplate,
  }
}
