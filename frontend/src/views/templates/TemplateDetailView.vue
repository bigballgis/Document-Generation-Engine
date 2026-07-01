<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import TemplateWorkflowBanner from '@/components/templates/TemplateWorkflowBanner.vue'
import TemplateAuthorJourneyBlock from '@/components/journey/TemplateAuthorJourneyBlock.vue'
import TemplateTesterJourneyBlock from '@/components/journey/TemplateTesterJourneyBlock.vue'
import TemplateApproverJourneyBlock from '@/components/journey/TemplateApproverJourneyBlock.vue'
import TemplateTeamLeadJourneyBlock from '@/components/journey/TemplateTeamLeadJourneyBlock.vue'
import TemplatePublishSummaryDialog from '@/components/templates/TemplatePublishSummaryDialog.vue'
import TemplateSubmitForApprovalSummaryDialog from '@/components/templates/TemplateSubmitForApprovalSummaryDialog.vue'
import TemplateLifecycleDecisionDialog, {
  type LifecycleDecisionDialogMode,
  type LifecycleDecisionSubmitPayload,
} from '@/components/templates/TemplateLifecycleDecisionDialog.vue'
import TemplateDevVersionActionBar from '@/components/templates/TemplateDevVersionActionBar.vue'
import TemplateExportActions from '@/components/templates/TemplateExportActions.vue'
import TemplateMetadataEditDialog from '@/components/templates/TemplateMetadataEditDialog.vue'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import TemplateDetailLifecycleTab from '@/views/templates/detail/TemplateDetailLifecycleTab.vue'
import TemplateDetailAuthoringTab from '@/views/templates/detail/TemplateDetailAuthoringTab.vue'
import TemplateDetailReleaseVersionsTab from '@/views/templates/detail/TemplateDetailReleaseVersionsTab.vue'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
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

const props = withDefaults(
  defineProps<{
    workspace?: 'legacy' | 'dev-editor'
  }>(),
  {
    workspace: 'legacy',
  },
)

const isDevEditor = computed(() => props.workspace === 'dev-editor')

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
type DetailTab = TemplateDetailTab

function resolveDevEditorInitialTab(): DetailTab {
  const tab = resolveTemplateDetailTabFromQuery(route.query)
  if (tab === 'overview' || tab === 'lifecycle') {
    return 'authoring'
  }
  return tab
}

const activeDetailTab = ref<DetailTab>(
  isDevEditor.value ? resolveDevEditorInitialTab() : resolveTemplateDetailTabFromQuery(route.query),
)
const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)

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
const showAuthoringSection = computed(
  () =>
    authorTemplates.value &&
    template.value?.lifecycleStatus !== 'PUBLISHED' &&
    template.value?.lifecycleStatus !== 'STOPPED' &&
    template.value?.lifecycleStatus !== 'DEPRECATED',
)
const showTestWorkflowRedirect = computed(
  () =>
    isDevEditor.value &&
    (template.value?.lifecycleStatus === 'DRAFT' ||
      template.value?.lifecycleStatus === 'TESTING') &&
    (authorTemplates.value || decideTests.value),
)
const showDevWorkflowBar = computed(
  () =>
    isDevEditor.value &&
    (showLifecycleSection.value ||
      showGovernanceSection.value ||
      showTestWorkflowRedirect.value),
)
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
  activeDetailTab.value = 'authoring'
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
  openLifecyclePanel()
}

function handleJourneyCheckEvidence() {
  openLifecyclePanel()
  testerEvidenceViewed.value = {
    fidelityViewedConfirmed: true,
    coverageViewedConfirmed: true,
    previewViewedConfirmed: false,
  }
}

function handleJourneyRecordResult() {
  openLifecyclePanel()
  testerEvidenceViewed.value = {
    fidelityViewedConfirmed: true,
    coverageViewedConfirmed: true,
    previewViewedConfirmed: true,
  }
}

function handleJourneyApproverReviewRequest() {
  openLifecyclePanel()
}

function handleJourneyApproverReviewSubmission() {
  openLifecyclePanel()
  approverEvidenceViewed.value = {
    ...approverEvidenceViewed.value,
    submissionReviewedConfirmed: true,
  }
}

function handleJourneyApproverRecordDecision() {
  openLifecyclePanel()
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
      activeDetailTab.value = 'authoring'
      scrollToDevVersionActions()
      const query = { ...normalized.query, tab: 'authoring', focus: 'workflow' }
      void router.replace({ query })
      return
    }
    activeDetailTab.value = normalized.tab
    scrollToLifecyclePanel()
    void router.replace({ query: normalized.query })
    return
  }

  if (isDevEditor.value && route.query.focus === 'workflow') {
    activeDetailTab.value = 'authoring'
    scrollToDevVersionActions()
    return
  }

  const tab = isDevEditor.value ? resolveDevEditorInitialTab() : resolveTemplateDetailTabFromQuery(route.query)
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
      ? resolveDevEditorInitialTab()
      : resolveTemplateDetailTabFromQuery(route.query)
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

function scrollToDevVersionActions() {
  void nextTick(() => {
    document.getElementById('dev-version-actions')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  })
}

function openLifecyclePanel() {
  if (isDevEditor.value) {
    activeDetailTab.value = 'authoring'
    scrollToDevVersionActions()
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
    activeDetailTab.value = 'authoring'
    await router.replace({
      query: { ...route.query, tab: 'authoring', authoringTab: 'testPreview' },
    })
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
    activeDetailTab.value = 'authoring'
    await router.replace({
      query: { ...route.query, tab: 'authoring', authoringTab: 'testPreview' },
    })
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
  activeDetailTab.value = 'authoring'
  void router.replace({
    query: { ...route.query, tab: 'authoring', authoringTab: 'testPreview' },
  })
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

async function submitLifecycleDecision(payload: LifecycleDecisionSubmitPayload) {
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

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

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
</script>

<template>
  <div class="template-detail-page">
    <header class="page-header">
      <el-button link type="primary" @click="backToList">
        {{
          isDevEditor
            ? t('templates.releaseDetail.backToHub')
            : t('templates.detail.backToList')
        }}
      </el-button>
      <div v-if="template" class="header-content">
        <div class="header-title-block">
          <el-tooltip :content="template.name" placement="top">
            <h1 class="template-name">{{ template.name }}</h1>
          </el-tooltip>
          <p>{{ t('templates.detail.groupLabel', { groupCode: template.groupCode }) }}</p>
        </div>
        <div class="header-actions">
          <TemplateExportActions
            v-if="showExportActions"
            :template-id="templateId"
            :external-id="template.externalId"
          />
          <el-button
            v-if="showDeleteTemplateAction"
            type="danger"
            plain
            :loading="templatesStore.submitting"
            @click="handleDeleteTemplate"
          >
            {{ t('templates.deleteAction.button') }}
          </el-button>
          <el-button v-if="showMetadataEdit" @click="metadataEditOpen = true">
            {{ t('templates.metadata.edit') }}
          </el-button>
          <TemplateStatusBadge
            :status="template.lifecycleStatus"
            :approval-sub-state="template.approvalSubState"
          />
        </div>
      </div>
    </header>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="templatesStore.lastErrorMessageKey ?? 'templates.error.loadDetail'"
      @retry="loadTemplate"
    />

    <el-skeleton v-else-if="showDetailSkeleton" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!template"
      title-key="templates.empty.notFoundTitle"
      description-key="templates.empty.notFoundDescription"
    />

    <template v-else-if="template">
      <TemplateAuthorJourneyBlock
        v-if="showAuthorJourney && authorJourneyContext"
        :journey-context="authorJourneyContext"
        :template-id="templateId"
        :can-write="authorTemplates"
        :primary-cta-disabled="authorJourneyPrimaryCtaDisabled"
        @create="handleJourneyCreate"
        @design="handleJourneyDesign"
        @trial-generate="handleJourneyTrialGenerate"
        @submit-for-test="handleJourneySubmitForTest"
        @submit-for-approval="handleJourneySubmitForApproval"
      />

      <TemplateTesterJourneyBlock
        v-if="showTesterJourney && testerJourneyContext"
        :journey-context="testerJourneyContext"
        :can-decide="decideTests"
        @review-request="handleJourneyReviewRequest"
        @check-evidence="handleJourneyCheckEvidence"
        @record-result="handleJourneyRecordResult"
      />

      <TemplateApproverJourneyBlock
        v-if="showApproverJourney && approverJourneyContext"
        :journey-context="approverJourneyContext"
        :can-decide="decideApprovals"
        @review-request="handleJourneyApproverReviewRequest"
        @review-submission="handleJourneyApproverReviewSubmission"
        @record-decision="handleJourneyApproverRecordDecision"
      />

      <TemplateTeamLeadJourneyBlock
        v-if="showTeamLeadJourney && teamLeadJourneyContext"
        :journey-context="teamLeadJourneyContext"
        :can-publish="publishTemplates"
        @review-go-live-request="handleJourneyTeamLeadReviewGoLiveRequest"
        @run-pre-release-checks="handleJourneyTeamLeadRunPreReleaseChecks"
        @confirm-go-live="handleJourneyTeamLeadConfirmGoLive"
      />

      <TemplateWorkflowBanner
        v-if="!(isDevEditor && showTestWorkflowRedirect)"
        :template="template"
        @open-lifecycle="openLifecyclePanel"
      />

      <TemplateDevVersionActionBar
        v-if="showDevWorkflowBar"
        :lifecycle-comment="lifecycleComment"
        :show-submit-for-approval="showSubmitForApproval"
        :show-approval-decision-actions="showApprovalDecisionActions"
        :show-publish-actions="showPublishActions"
        :show-test-workflow-redirect="showTestWorkflowRedirect"
        :show-stop-action="showStopAction"
        :show-restore-action="showRestoreAction"
        :show-deprecate-action="showDeprecateAction"
        :show-governance-section="showGovernanceSection"
        :publish-gate-items="publishGateItems"
        :loading-publish-gate="loadingPublishGate"
        :publish-bump-level="publishBumpLevel"
        :publish-version-conflict="publishVersionConflict"
        :publish-gate-ready="publishGateReady"
        :publish-bump-options="publishBumpOptions"
        :binding-gate-result="bindingGateResult"
        :publish-gate-load-error="publishGateLoadError"
        :submit-gate-items="submitGateItems"
        :loading-submit-gate="loadingSubmitGate"
        :submit-gate-ready="submitGateReady"
        :submit-gate-load-error="submitGateLoadError"
        :submitting="templatesStore.submitting"
        @update:lifecycle-comment="lifecycleComment = $event"
        @update:publish-bump-level="publishBumpLevel = $event"
        @submit-for-approval="handleSubmitForApproval"
        @approval-decision="handleApprovalDecision"
        @publish="handlePublish"
        @open-test-preview-tab="openTestPreviewTab"
        @governance-action="handleGovernanceAction"
        @retry-publish-gate="loadPublishGateData"
        @retry-submit-gate="loadSubmitGateData"
      />

      <el-tabs v-model="activeDetailTab" class="detail-tabs">
        <el-tab-pane
          v-if="!isDevEditor"
          :label="t(templateDetailTabLabelKey('overview'))"
          name="overview"
        >
          <TemplateDetailOverviewTab :template="template" :format-date-time="formatDateTime" />
        </el-tab-pane>

        <el-tab-pane
          v-if="!isDevEditor"
          :label="t(templateDetailTabLabelKey('lifecycle'))"
          name="lifecycle"
        >
          <TemplateDetailLifecycleTab
            :template-id="templateId"
            :show-lifecycle-section="showLifecycleSection"
            :show-governance-section="showGovernanceSection"
            :lifecycle-comment="lifecycleComment"
            :show-draft-actions="showDraftActions"
            :show-testing-decision-actions="showTestingDecisionActions"
            :show-submit-for-approval="showSubmitForApproval"
            :show-approval-decision-actions="showApprovalDecisionActions"
            :show-publish-actions="showPublishActions"
            :show-test-generate="showTestGenerate"
            :show-stop-action="showStopAction"
            :show-restore-action="showRestoreAction"
            :show-deprecate-action="showDeprecateAction"
            :publish-gate-items="publishGateItems"
            :loading-publish-gate="loadingPublishGate"
            :publish-bump-level="publishBumpLevel"
            :publish-version-conflict="publishVersionConflict"
            :publish-gate-ready="publishGateReady"
            :publish-bump-options="publishBumpOptions"
            :binding-gate-result="bindingGateResult"
            :publish-gate-load-error="publishGateLoadError"
            :submit-gate-items="submitGateItems"
            :loading-submit-gate="loadingSubmitGate"
            :submit-gate-ready="submitGateReady"
            :submit-gate-load-error="submitGateLoadError"
            :submitting="templatesStore.submitting"
            @update:lifecycle-comment="lifecycleComment = $event"
            @update:publish-bump-level="publishBumpLevel = $event"
            @submit-for-test="handleSubmitForTest"
            @test-decision="handleTestDecision"
            @submit-for-approval="handleSubmitForApproval"
            @approval-decision="handleApprovalDecision"
            @publish="handlePublish"
            @test-generate="handleTestGenerate"
            @governance-action="handleGovernanceAction"
            @retry-publish-gate="loadPublishGateData"
            @retry-submit-gate="loadSubmitGateData"
          />
        </el-tab-pane>

        <el-tab-pane
          v-if="showAuthoringSection"
          :label="t(templateDetailTabLabelKey('authoring'))"
          name="authoring"
        >
          <TemplateDetailAuthoringTab
            :template-id="templateId"
            :master-id="template.masterId"
            :variables="template.variables"
            :bindings="template.bindings"
            :rules="template.rules"
            :group-code="template.groupCode"
            :can-edit-content-module-references="canEditContentModuleReferences"
            :coverage-refresh-token="coverageRefreshToken"
            :last-preview="lastPreview"
            :selected-preview-id="selectedPreviewId"
            :lifecycle-status="template.lifecycleStatus"
            :selected-test-data-set-id="selectedTestDataSetId"
            :show-draft-actions="showDraftActions"
            :show-testing-decision-actions="showTestingDecisionActions"
            :show-test-generate="showTestGenerate"
            :submitting="templatesStore.submitting"
            :generating-preview="generatingPreview"
            :generating-preview-id="generatingPreviewId"
            :batch-testing="batchTesting"
            :open-submit-for-test-dialog="submitForTestDialogOpen"
            @updated="loadTemplate"
            @update:selected-test-data-set-id="selectedTestDataSetId = $event"
            @update:selected-preview-id="handlePreviewSelected"
            @update:open-submit-for-test-dialog="submitForTestDialogOpen = $event"
            @test-generate="handleTestGenerate"
            @test-generate-batch="handleBatchTestGenerate"
            @submit-for-test="handleSubmitForTest"
            @test-decision="handleTestDecision"
            @batch-complete="coverageRefreshToken += 1"
          />
        </el-tab-pane>

        <el-tab-pane
          v-if="!isDevEditor"
          :label="t(templateDetailTabLabelKey('releaseVersions'))"
          name="releaseVersions"
        >
          <TemplateDetailReleaseVersionsTab
            :template-id="templateId"
            :template-lifecycle-status="template.lifecycleStatus"
            @changed="loadTemplate"
          />
        </el-tab-pane>

        <el-tab-pane
          v-if="showPolicyPanel && !isDevEditor"
          :label="t(templateDetailTabLabelKey('apiAccess'))"
          name="apiAccess"
        >
          <TemplateDetailApiAccessTab
            v-model:credential-column-filters="credentialColumnFilters"
            v-model:credentials-current-page="credentialsCurrentPage"
            v-model:selected-contract-environment="selectedContractEnvironment"
            :template-id="templateId"
            :show-policy-panel="showPolicyPanel"
            :loading-policy="templatesStore.loadingPolicy"
            :api-policy="templatesStore.apiPolicy"
            :policy-load-failed="policyLoadFailed"
            :policy-load-error-key="templatesStore.lastErrorMessageKey"
            :paginated-credentials="paginatedCredentials"
            :credential-status-filter-options="credentialStatusFilterOptions"
            :page-size="CLIENT_TABLE_PAGE_SIZE"
            :total-credential-rows="totalCredentialRows"
            :submitting="templatesStore.submitting"
            :format-date-time="formatDateTime"
            :sort-credentials-by-created-at="sortCredentialsByCreatedAt"
            @open-api-policy-console="openApiPolicyConsole"
            @create-credential="handleCreateCredential"
            @rotate-credential="handleRotateCredential"
            @revoke-credential="handleRevokeCredential"
            @retry-policy-load="loadPolicyData"
          />
        </el-tab-pane>
      </el-tabs>
    </template>

    <TemplateMetadataEditDialog
      v-if="template"
      v-model="metadataEditOpen"
      :initial-name="template.name"
      :initial-description="template.description"
      :loading="templatesStore.submitting"
      @submit="handleMetadataUpdate"
    />

    <TemplatePublishSummaryDialog
      v-if="template"
      v-model="publishSummaryOpen"
      :template-name="template.name"
      :release-version="publishVersion"
      :gate-items="publishGateItems"
      :coverage-summary="publishCoverageSummary"
      :change-diff-summary="publishChangeDiffSummary"
      :preview-comparison="lastPreview?.previewComparison ?? null"
      :loading="templatesStore.submitting"
      @confirm="confirmPublishFromSummary"
    />

    <TemplateSubmitForApprovalSummaryDialog
      v-if="template"
      v-model="submitSummaryOpen"
      :template-name="template.name"
      :gate-items="submitGateItems"
      :coverage-summary="submitCoverageSummary"
      :change-diff-summary="submitChangeDiffSummary"
      :preview-comparison="lastPreview?.previewComparison ?? null"
      :loading="templatesStore.submitting"
      @confirm="confirmSubmitFromSummary"
    />

    <TemplateLifecycleDecisionDialog
      v-model="decisionDialogOpen"
      :mode="decisionDialogMode"
      :template-id="templateId"
      :loading="templatesStore.submitting"
      :initial-comment="lifecycleComment"
      @submit="submitLifecycleDecision"
    />

    <el-dialog
      v-model="credentialSecretDialogVisible"
      :title="t('templates.policy.credentialSecretDialogTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <p>{{ t('templates.policy.credentialSecretHint') }}</p>
      <p>{{ t('templates.policy.credentialExternalId') }}: {{ credentialSecretExternalId }}</p>
      <el-input
        :model-value="displayedCredentialSecret || credentialSecretValue"
        readonly
        type="textarea"
        :rows="3"
      />
      <template #footer>
        <el-button type="primary" @click="credentialSecretDialogVisible = false">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.template-detail-page {
  padding: 2rem;
}

.page-header {
  margin-bottom: 1.5rem;
}

.detail-tabs {
  margin-top: 0.25rem;
}

.header-content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.5rem;
}

.header-title-block {
  min-width: 0;
  flex: 1 1 auto;
}

.template-name {
  margin: 0 0 0.25rem;
  font-size: 1.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.header-title-block p {
  margin: 0;
  color: var(--text-muted);
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}
</style>
