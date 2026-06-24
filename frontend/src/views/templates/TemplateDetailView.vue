<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import TemplateAuthoringPanel from '@/components/templates/TemplateAuthoringPanel.vue'
import TemplateRuleConfigurator from '@/components/templates/TemplateRuleConfigurator.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import TemplateTestDataSetPanel from '@/components/templates/TemplateTestDataSetPanel.vue'
import TemplateMetadataEditDialog from '@/components/templates/TemplateMetadataEditDialog.vue'
import TemplateReleaseVersionHistoryPanel from '@/components/templates/TemplateReleaseVersionHistoryPanel.vue'
import TemplateWorkflowBanner from '@/components/templates/TemplateWorkflowBanner.vue'
import TemplatePublishSummaryDialog from '@/components/templates/TemplatePublishSummaryDialog.vue'
import TemplateLifecycleDecisionDialog, {
  type LifecycleDecisionDialogMode,
} from '@/components/templates/TemplateLifecycleDecisionDialog.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCredentialStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { MASTER_DETAIL_PATH_PREFIX, ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import { conflictsWithExisting, suggestNextVersions, type SemverBumpLevel } from '@/utils/semver'
import { resolveTemplateDetailTab, type TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type {
  ApiCredentialSummary,
  BindingValidationResult,
  DeleteTemplatePayload,
  LifecycleGovernanceAction,
  PreviewRecord,
  UpsertApiPolicyPayload,
} from '@/types/template'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const route = useRoute()
const router = useRouter()
const templatesStore = useTemplatesStore()
const {
  authorTemplates,
  decideTests,
  decideApprovals,
  publishTemplates,
  stopTemplates,
  restoreOrDeprecateTemplates,
  manageApiPolicy,
  deleteTemplates,
} = useCapabilities()
const { confirmAction } = useConfirmAction()

const lifecycleComment = ref('')
const decisionDialogOpen = ref(false)
const decisionDialogMode = ref<LifecycleDecisionDialogMode>('test-fail')
const publishBumpLevel = ref<SemverBumpLevel>('patch')
const publishVersion = ref('1.0.0')
const publishSummaryOpen = ref(false)
const publishedReleaseVersions = ref<string[]>([])
const credentialSecretDialogVisible = ref(false)
const credentialSecretValue = ref('')
const credentialSecretExternalId = ref('')
const lastPreview = ref<PreviewRecord | null>(null)
const selectedTestDataSetId = ref<string | null>(null)
const bindingGateResult = ref<BindingValidationResult | null>(null)
const loadingPublishGate = ref(false)
const metadataEditOpen = ref(false)
const loadFailed = ref(false)
type DetailTab = TemplateDetailTab

function resolveDetailTab(value: unknown): DetailTab {
  return resolveTemplateDetailTab(value)
}

const activeDetailTab = ref<DetailTab>(resolveDetailTab(route.query.tab))
const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)

const policyOutputFormatOptions = ['DOCX', 'PDF']
const policyOutputModeOptions = ['SYNC_STREAM', 'ASYNC_CALLBACK', 'INLINE']

const policyForm = reactive<UpsertApiPolicyPayload>({
  allowedAdGroups: [],
  defaultRouteReleaseVersion: '1.0.0',
  outputFormats: ['PDF'],
  outputModes: ['INLINE'],
  batchEnabled: false,
  maxBatchSize: 10,
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: false,
})

const templateId = computed(() => route.params.templateId as string)

const credentialsSource = computed(() => templatesStore.credentials)
const { filters: credentialColumnFilters, filteredRows: filteredCredentials } = useDataTableFilters(
  credentialsSource,
  [
    { key: 'externalId', getValue: (row) => row.externalId },
    { key: 'status', getValue: (row) => row.status, matchMode: 'exact' },
    { key: 'createdAt', getValue: (row) => formatDateTime(row.createdAt) },
  ],
)
const sortCredentialsByCreatedAt = rowSortMethod<ApiCredentialSummary>((row) => row.createdAt)
const credentialStatusFilterOptions = useCredentialStatusFilterOptions()
const template = computed(() => templatesStore.selectedTemplate)
const canPolicy = computed(() => manageApiPolicy.value)

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

const approvalSubState = computed(() => template.value?.approvalSubState)

const showDraftActions = computed(
  () => template.value?.lifecycleStatus === 'DRAFT' && authorTemplates.value,
)
const showTestingDecisionActions = computed(
  () => template.value?.lifecycleStatus === 'TESTING' && decideTests.value,
)
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
const showPublishActions = computed(
  () => template.value?.lifecycleStatus === 'PENDING_RELEASE' && publishTemplates.value,
)
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
  if (!status || !authorTemplates.value) {
    return false
  }
  return status !== 'PUBLISHED' && status !== 'STOPPED' && status !== 'DEPRECATED'
})
const showDeleteTemplateAction = computed(
  () => deleteTemplates.value && template.value?.lifecycleStatus !== 'DELETED',
)

const publishGateItems = computed(() => [
  {
    key: 'lifecycle',
    label: t('templates.publishGate.lifecycleReady'),
    ready: template.value?.lifecycleStatus === 'PENDING_RELEASE',
    informational: false,
  },
  {
    key: 'releaseVersion',
    label: t('templates.publishGate.releaseVersionProvided'),
    ready: Boolean(publishVersion.value.trim()),
    informational: false,
  },
  {
    key: 'bindings',
    label: t('templates.publishGate.bindingsValid'),
    ready: Boolean(bindingGateResult.value && !bindingGateResult.value.summary.blocking),
    informational: false,
  },
  {
    key: 'apiPolicy',
    label: t('templates.publishGate.apiPolicyConfigured'),
    ready: Boolean(templatesStore.apiPolicy),
    informational: false,
  },
])

const suggestedVersions = computed(() =>
  suggestNextVersions(template.value?.releaseVersion ?? null),
)

const publishVersionConflict = computed(() =>
  conflictsWithExisting(publishVersion.value, publishedReleaseVersions.value),
)

const publishGateReady = computed(() =>
  publishGateItems.value.filter((item) => !item.informational).every((item) => item.ready) &&
  !publishVersionConflict.value,
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

onMounted(async () => {
  await loadTemplate()
})

watch(
  () => route.query.tab,
  (tab) => {
    const resolved = resolveDetailTab(tab)
    if (activeDetailTab.value !== resolved) {
      activeDetailTab.value = resolved
    }
  },
)

watch(activeDetailTab, (tab) => {
  if (resolveDetailTab(route.query.tab) === tab) {
    return
  }
  router.replace({ query: { ...route.query, tab } })
})

watch(
  showPublishActions,
  async (active) => {
    if (!active) {
      bindingGateResult.value = null
      publishedReleaseVersions.value = []
      return
    }
    publishBumpLevel.value = 'patch'
    publishVersion.value = suggestedVersions.value.patch
    loadingPublishGate.value = true
    try {
      await templatesStore.fetchApiPolicy(templateId.value)
      bindingGateResult.value = await templatesStore.validateBindings(templateId.value)
      const versions = await templatesApi.fetchReleaseVersions(templateId.value)
      publishedReleaseVersions.value = versions.map((entry) => entry.releaseVersion)
    } catch {
      bindingGateResult.value = null
      publishedReleaseVersions.value = []
    } finally {
      loadingPublishGate.value = false
    }
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
    await templatesStore.fetchTemplate(templateId.value)
    if (showPolicyPanel.value) {
      await loadPolicyData()
    }
  } catch {
    loadFailed.value = true
  }
}

function openLifecyclePanel() {
  activeDetailTab.value = 'overview'
  void nextTick(() => {
    document.getElementById('template-lifecycle-panel')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  })
}

async function loadPolicyData() {
  await Promise.all([
    templatesStore.fetchApiPolicy(templateId.value),
    templatesStore.fetchCredentials(templateId.value),
  ])
  if (templatesStore.apiPolicy) {
    policyForm.allowedAdGroups = [...templatesStore.apiPolicy.allowedAdGroups]
    policyForm.defaultRouteReleaseVersion = templatesStore.apiPolicy.defaultRouteReleaseVersion
    policyForm.outputFormats = [...templatesStore.apiPolicy.outputFormats]
    policyForm.outputModes = [...templatesStore.apiPolicy.outputModes]
    policyForm.batchEnabled = templatesStore.apiPolicy.batchEnabled
    policyForm.maxBatchSize = templatesStore.apiPolicy.maxBatchSize
    policyForm.docxEncryptionEnabled = templatesStore.apiPolicy.docxEncryptionEnabled
    policyForm.pdfEncryptionEnabled = templatesStore.apiPolicy.pdfEncryptionEnabled
  }
}

function backToList() {
  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
}

function openCredentialSecretDialog(externalId: string, secret: string) {
  credentialSecretExternalId.value = externalId
  credentialSecretValue.value = secret
  credentialSecretDialogVisible.value = true
}

async function handleTestGenerate() {
  try {
    const preview = await templatesStore.testGenerate(templateId.value, {
      testDataSetId: selectedTestDataSetId.value ?? undefined,
    })
    lastPreview.value = preview
    ElMessage.success(t('templates.testGenerate.success', { previewId: preview.previewId }))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.testGenerate'))
  }
}

async function handleSubmitForTest() {
  try {
    await templatesStore.submitForTest(templateId.value, { commentSummary: lifecycleComment.value })
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
  try {
    await templatesStore.recordTestDecision(templateId.value, {
      decision,
      commentSummary: lifecycleComment.value || undefined,
    })
    lifecycleComment.value = ''
    ElMessage.success(t('templates.lifecycle.testDecisionSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
  }
}

function openApprovalRejectDialog() {
  decisionDialogMode.value = 'approval-reject'
  decisionDialogOpen.value = true
}

async function submitNegativeLifecycleDecision(payload: {
  reasonCategory: string
  impactSummary: string
  commentSummary?: string
}) {
  const isTestFail = decisionDialogMode.value === 'test-fail'
  try {
    if (isTestFail) {
      await templatesStore.recordTestDecision(templateId.value, {
        decision: 'FAILED',
        reasonCategory: payload.reasonCategory,
        impactSummary: payload.impactSummary,
        commentSummary: payload.commentSummary,
      })
      ElMessage.success(t('templates.lifecycle.testDecisionSuccess'))
    } else {
      await templatesStore.recordApprovalDecision(templateId.value, {
        decision: 'REJECTED',
        reasonCategory: payload.reasonCategory,
        impactSummary: payload.impactSummary,
        commentSummary: payload.commentSummary,
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
  try {
    await templatesStore.recordApprovalDecision(templateId.value, {
      decision,
      commentSummary: lifecycleComment.value || undefined,
    })
    lifecycleComment.value = ''
    ElMessage.success(t('templates.lifecycle.approvalDecisionSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
  }
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
    activeDetailTab.value = 'releaseVersions'
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

async function handleSavePolicy() {
  const payload = { ...policyForm }
  try {
    const impactMessage = await buildPolicyImpactPreviewMessage(payload)
    await ElMessageBox.confirm(impactMessage, t('templates.policy.impact.title'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await templatesStore.saveApiPolicy(templateId.value, payload)
    ElMessage.success(t('templates.policy.saveSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.savePolicy'))
  }
}

async function buildPolicyImpactPreviewMessage(payload: UpsertApiPolicyPayload): Promise<string> {
  try {
    const impact = await templatesStore.previewApiPolicyImpact(templateId.value, payload)
    const currentVersion = impact.currentPolicyVersion ?? templatesStore.apiPolicy?.policyVersion ?? 0
    const nextVersion = impact.nextPolicyVersion ?? currentVersion + 1
    const fields =
      impact.changedFields.length > 0
        ? impact.changedFields.join(', ')
        : t('templates.policy.impact.noFieldChange')
    return [
      t('templates.policy.impact.versionChange', {
        currentVersion,
        nextVersion,
      }),
      t('templates.policy.impact.changedFields', { fields }),
      t('templates.policy.impact.confirmPrompt'),
    ].join('\n\n')
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.previewPolicyImpact'))
    throw new Error('policy-impact-preview-failed')
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
        {{ t('templates.detail.backToList') }}
      </el-button>
      <div v-if="template" class="header-content">
        <div>
          <h1>{{ template.name }}</h1>
          <p>{{ t('templates.detail.groupLabel', { groupCode: template.groupCode }) }}</p>
        </div>
        <div class="header-actions">
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
          <TemplateStatusBadge :status="template.lifecycleStatus" />
        </div>
      </div>
    </header>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="templatesStore.lastErrorMessageKey ?? 'templates.error.loadDetail'"
      @retry="loadTemplate"
    />

    <el-skeleton v-else-if="templatesStore.loadingDetail" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!template"
      title-key="templates.empty.notFoundTitle"
      description-key="templates.empty.notFoundDescription"
    />

    <template v-else-if="template">
      <TemplateWorkflowBanner :template="template" @open-lifecycle="openLifecyclePanel" />

      <el-tabs v-model="activeDetailTab" class="detail-tabs">
        <el-tab-pane :label="t('templates.detail.tabs.releaseVersions')" name="releaseVersions">
          <TemplateReleaseVersionHistoryPanel
            :template-id="templateId"
            :template-lifecycle-status="template.lifecycleStatus"
            @changed="loadTemplate"
          />
        </el-tab-pane>

        <el-tab-pane :label="t('templates.detail.tabs.overview')" name="overview">
      <el-card shadow="never" class="section-card">
        <h2>{{ t('templates.detail.summaryTitle') }}</h2>
        <dl class="summary-grid">
          <div>
            <dt>{{ t('templates.detail.externalId') }}</dt>
            <dd>{{ template.externalId }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.detail.masterId') }}</dt>
            <dd>
              <router-link :to="`${MASTER_DETAIL_PATH_PREFIX}${template.masterId}`">
                {{ template.masterId }}
              </router-link>
            </dd>
          </div>
          <div>
            <dt>{{ t('templates.detail.releaseVersion') }}</dt>
            <dd>{{ template.releaseVersion ?? t('templates.detail.noReleaseVersion') }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.detail.updatedAt') }}</dt>
            <dd>{{ formatDateTime(template.updatedAt) }}</dd>
          </div>
        </dl>
        <p class="description">
          {{ template.description ?? t('templates.detail.noDescription') }}
        </p>
      </el-card>

      <el-card
        v-if="showLifecycleSection"
        id="template-lifecycle-panel"
        shadow="never"
        class="section-card"
      >
        <h2>{{ t('templates.lifecycle.title') }}</h2>
        <el-input
          v-model="lifecycleComment"
          type="textarea"
          :rows="2"
          :placeholder="t('templates.lifecycle.commentPlaceholder')"
          class="lifecycle-comment"
        />
        <div class="action-row">
          <el-button
            v-if="showDraftActions"
            type="primary"
            :loading="templatesStore.submitting"
            @click="handleSubmitForTest"
          >
            {{ t('templates.lifecycle.submitTest') }}
          </el-button>
          <template v-if="showTestingDecisionActions">
            <el-button
              type="success"
              :loading="templatesStore.submitting"
              @click="handleTestDecision('PASSED')"
            >
              {{ t('templates.lifecycle.passTest') }}
            </el-button>
            <el-button
              type="danger"
              :loading="templatesStore.submitting"
              @click="handleTestDecision('FAILED')"
            >
              {{ t('templates.lifecycle.failTest') }}
            </el-button>
          </template>
          <el-button
            v-if="showSubmitForApproval"
            type="primary"
            :loading="templatesStore.submitting"
            @click="handleSubmitForApproval"
          >
            {{ t('templates.lifecycle.submitApproval') }}
          </el-button>
          <template v-if="showApprovalDecisionActions">
            <el-button
              type="success"
              :loading="templatesStore.submitting"
              @click="handleApprovalDecision('APPROVED')"
            >
              {{ t('templates.lifecycle.approve') }}
            </el-button>
            <el-button
              type="danger"
              :loading="templatesStore.submitting"
              @click="handleApprovalDecision('REJECTED')"
            >
              {{ t('templates.lifecycle.reject') }}
            </el-button>
          </template>
          <template v-if="showPublishActions">
            <el-card shadow="never" class="publish-gate-card">
              <h3>{{ t('templates.publishGate.title') }}</h3>
              <p>{{ t('templates.publishGate.description') }}</p>
              <el-skeleton v-if="loadingPublishGate" :rows="3" animated />
              <ul v-else class="publish-gate-list">
                <li v-for="item in publishGateItems" :key="item.key">
                  <span>{{ item.label }}</span>
                  <el-tag
                    v-if="item.informational"
                    type="info"
                    size="small"
                  >
                    {{ t('templates.publishGate.informational') }}
                  </el-tag>
                  <el-tag
                    v-else
                    :type="item.ready ? 'success' : 'warning'"
                    size="small"
                  >
                    {{ item.ready ? t('templates.publishGate.ready') : t('templates.publishGate.pending') }}
                  </el-tag>
                </li>
              </ul>
            </el-card>
            <el-radio-group v-model="publishBumpLevel" class="publish-bump-picker">
              <el-radio-button
                v-for="option in publishBumpOptions"
                :key="option.level"
                :value="option.level"
              >
                {{ option.label }} ({{ option.version }})
              </el-radio-button>
            </el-radio-group>
            <el-alert
              v-if="publishVersionConflict"
              class="publish-conflict-alert"
              type="warning"
              :title="t('templates.lifecycle.releaseVersionConflict')"
              show-icon
              :closable="false"
            />
            <el-button
              type="primary"
              :loading="templatesStore.submitting"
              :disabled="!publishGateReady"
              @click="handlePublish"
            >
              {{ t('templates.lifecycle.publish') }}
            </el-button>
          </template>
          <el-button
            v-if="showTestGenerate"
            :loading="templatesStore.submitting"
            @click="handleTestGenerate"
          >
            {{ t('templates.testGenerate.action') }}
          </el-button>
        </div>
      </el-card>

      <el-card v-if="showGovernanceSection" shadow="never" class="section-card">
        <h2>{{ t('templates.governance.title') }}</h2>
        <p class="governance-description">{{ t('templates.governance.description') }}</p>
        <div class="action-row">
          <el-button
            v-if="showStopAction"
            type="warning"
            :loading="templatesStore.submitting"
            @click="handleGovernanceAction('stop')"
          >
            {{ t('templates.governance.stop') }}
          </el-button>
          <el-button
            v-if="showRestoreAction"
            type="primary"
            :loading="templatesStore.submitting"
            @click="handleGovernanceAction('restore')"
          >
            {{ t('templates.governance.restore') }}
          </el-button>
          <el-button
            v-if="showDeprecateAction"
            type="danger"
            :loading="templatesStore.submitting"
            @click="handleGovernanceAction('deprecate')"
          >
            {{ t('templates.governance.deprecate') }}
          </el-button>
        </div>
      </el-card>
        </el-tab-pane>

        <el-tab-pane
          v-if="showAuthoringSection"
          :label="t('templates.detail.tabs.authoring')"
          name="authoring"
        >
      <el-card shadow="never" class="section-card">
        <h2>{{ t('templates.authoring.title') }}</h2>
        <TemplateAuthoringPanel
          :template-id="templateId"
          :variables="template.variables"
          :bindings="template.bindings"
          @updated="loadTemplate"
        />
        <TemplateRuleConfigurator
          :template-id="templateId"
          :initial-rules="template.rules ?? []"
          @updated="loadTemplate"
        />
        <h3>{{ t('templates.testDataSets.title') }}</h3>
        <TemplateTestDataSetPanel
          :template-id="templateId"
          @selected="(id) => { selectedTestDataSetId = id }"
        />
      </el-card>

      <el-card v-if="showAuthoringSection" shadow="never" class="section-card">
        <h2>{{ t('templates.preview.title') }}</h2>
        <TemplatePreviewPanel
          :template-id="templateId"
          :bindings="template.bindings"
          :preview="lastPreview"
        />
      </el-card>
        </el-tab-pane>

        <el-tab-pane
          v-if="showPolicyPanel"
          :label="t('templates.detail.tabs.apiAccess')"
          name="apiAccess"
        >
      <el-card shadow="never" class="section-card">
        <h2>{{ t('templates.policy.title') }}</h2>
        <el-skeleton v-if="templatesStore.loadingPolicy" :rows="4" animated />
        <template v-else>
          <div class="policy-form">
            <el-form-item
              v-if="templatesStore.apiPolicy"
              :label="t('templates.policy.policyVersion')"
            >
              <el-input :model-value="String(templatesStore.apiPolicy.policyVersion)" readonly />
            </el-form-item>
            <el-form-item :label="t('templates.policy.defaultRouteReleaseVersion')">
              <el-input v-model="policyForm.defaultRouteReleaseVersion" />
            </el-form-item>
            <el-form-item :label="t('templates.policy.allowedAdGroups')">
              <AppSearchSelect
                v-model="policyForm.allowedAdGroups"
                multiple
                filterable
                allow-create
                default-first-option
                :placeholder="t('templates.policy.allowedAdGroupsPlaceholder')"
              />
            </el-form-item>
            <el-form-item :label="t('templates.policy.outputFormats')">
              <AppSearchSelect v-model="policyForm.outputFormats" multiple filterable allow-create>
                <el-option
                  v-for="format in policyOutputFormatOptions"
                  :key="format"
                  :label="format"
                  :value="format"
                />
              </AppSearchSelect>
            </el-form-item>
            <el-form-item :label="t('templates.policy.outputModes')">
              <AppSearchSelect v-model="policyForm.outputModes" multiple filterable allow-create>
                <el-option
                  v-for="mode in policyOutputModeOptions"
                  :key="mode"
                  :label="mode"
                  :value="mode"
                />
              </AppSearchSelect>
            </el-form-item>
            <el-form-item :label="t('templates.policy.batchEnabled')">
              <el-switch v-model="policyForm.batchEnabled" />
            </el-form-item>
            <el-form-item :label="t('templates.policy.maxBatchSize')">
              <el-input-number v-model="policyForm.maxBatchSize" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item :label="t('templates.policy.docxEncryptionEnabled')">
              <el-switch v-model="policyForm.docxEncryptionEnabled" />
            </el-form-item>
            <el-form-item :label="t('templates.policy.pdfEncryptionEnabled')">
              <el-switch v-model="policyForm.pdfEncryptionEnabled" />
            </el-form-item>
          </div>
          <div class="action-row">
            <el-button type="primary" :loading="templatesStore.submitting" @click="handleSavePolicy">
              {{ t('templates.policy.save') }}
            </el-button>
            <el-button :loading="templatesStore.submitting" @click="handleCreateCredential">
              {{ t('templates.policy.createCredential') }}
            </el-button>
          </div>
          <h3>{{ t('templates.policy.credentialsTitle') }}</h3>
          <AppDataTable :data="filteredCredentials" empty-text="">
            <template #empty>
              <el-empty :description="t('templates.policy.noCredentials')" />
            </template>
            <el-table-column prop="externalId" sortable>
              <template #header>
                <TableColumnHeader
                  :label="t('templates.policy.credentialExternalId')"
                  v-model="credentialColumnFilters.externalId"
                />
              </template>
            </el-table-column>
            <el-table-column prop="status" sortable>
              <template #header>
                <TableColumnHeader
                  :label="t('templates.policy.credentialStatus')"
                  v-model="credentialColumnFilters.status"
                  filter-type="select"
                  :options="credentialStatusFilterOptions"
                />
              </template>
            </el-table-column>
            <el-table-column sortable :sort-method="sortCredentialsByCreatedAt" min-width="180">
              <template #header>
                <TableColumnHeader
                  :label="t('templates.policy.credentialCreatedAt')"
                  v-model="credentialColumnFilters.createdAt"
                />
              </template>
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('templates.policy.credentialActions')" min-width="200">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  link
                  type="primary"
                  @click="handleRotateCredential(row.credentialId, row.externalId)"
                >
                  {{ t('templates.policy.rotateCredential') }}
                </el-button>
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  link
                  type="danger"
                  @click="handleRevokeCredential(row.credentialId)"
                >
                  {{ t('templates.policy.revokeCredential') }}
                </el-button>
              </template>
            </el-table-column>
          </AppDataTable>
        </template>
      </el-card>

      <el-card v-if="showPolicyPanel" shadow="never" class="section-card">
        <h2>{{ t('templates.contract.title') }}</h2>
        <TemplateCallerContractPanel
          :template-id="templateId"
          :environment="selectedContractEnvironment"
          @update:environment="selectedContractEnvironment = $event"
        />
      </el-card>
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
      :bindings-ready="Boolean(bindingGateResult && !bindingGateResult.summary.blocking)"
      :loading="templatesStore.submitting"
      @confirm="confirmPublishFromSummary"
    />

    <TemplateLifecycleDecisionDialog
      v-model="decisionDialogOpen"
      :mode="decisionDialogMode"
      :loading="templatesStore.submitting"
      :initial-comment="lifecycleComment"
      @submit="submitNegativeLifecycleDecision"
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

  h1 {
    margin: 0 0 0.25rem;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.governance-description {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.page-alert {
  margin-bottom: 1rem;
}

.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }

  h3 {
    margin: 1.5rem 0 0.75rem;
    font-size: 1rem;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0 0 1rem;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}

.description {
  margin: 0;
  color: var(--text-muted);
}

.lifecycle-comment {
  margin-bottom: 1rem;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.publish-bump-picker {
  width: 100%;
}

.publish-conflict-alert {
  width: 100%;
}

.publish-gate-card {
  width: 100%;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border-subtle, #e5e7eb);
  border-radius: 8px;

  h3 {
    margin: 0 0 0.5rem;
    font-size: 1rem;
  }

  p {
    margin: 0 0 0.75rem;
    color: var(--text-muted);
  }
}

.publish-gate-list {
  margin: 0;
  padding-left: 1.25rem;
}

.policy-form {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 0.75rem 1rem;
  margin-bottom: 1rem;
}
</style>
