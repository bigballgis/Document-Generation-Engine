<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { MASTER_DETAIL_PATH_PREFIX, ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type {
  BindingValidationResult,
  DeleteTemplatePayload,
  LifecycleGovernanceAction,
  PreviewRecord,
  TemplateLifecycleStatus,
  UpsertApiPolicyPayload,
} from '@/types/template'

const { t, te } = useI18n()
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
  manageReleaseVersionState,
  manageApiPolicy,
  deleteTemplates,
} = useCapabilities()
const { confirmAction } = useConfirmAction()

const lifecycleComment = ref('')
const publishVersion = ref('1.0.0')
const credentialSecretDialogVisible = ref(false)
const credentialSecretValue = ref('')
const credentialSecretExternalId = ref('')
const lastPreview = ref<PreviewRecord | null>(null)
const selectedTestDataSetId = ref<string | null>(null)
const bindingGateResult = ref<BindingValidationResult | null>(null)
const loadingPublishGate = ref(false)
const metadataEditOpen = ref(false)
const loadFailed = ref(false)
const versionStatuses = ref<Record<string, TemplateLifecycleStatus>>({})
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
const showVersionsSection = computed(
  () =>
    manageReleaseVersionState.value &&
    template.value?.lifecycleStatus === 'PUBLISHED' &&
    Boolean(template.value.releaseVersion),
)
const publishedVersions = computed(() => {
  const releaseVersion = template.value?.releaseVersion
  if (!releaseVersion) {
    return []
  }
  return [
    {
      releaseVersion,
      lifecycleStatus: versionStatuses.value[releaseVersion] ?? 'PUBLISHED',
    },
  ]
})

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
    ready: true,
    informational: true,
  },
])

const publishGateReady = computed(() =>
  publishGateItems.value.filter((item) => !item.informational).every((item) => item.ready),
)

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
  showPublishActions,
  async (active) => {
    if (!active) {
      bindingGateResult.value = null
      return
    }
    loadingPublishGate.value = true
    try {
      bindingGateResult.value = await templatesStore.validateBindings(templateId.value)
    } catch {
      bindingGateResult.value = null
    } finally {
      loadingPublishGate.value = false
    }
  },
  { immediate: true },
)

async function syncVersionStatusFromPreview(releaseVersion: string) {
  try {
    const preview = await templatesStore.fetchLifecycleImpactPreview(templateId.value, {
      action: 'DEACTIVATE_VERSION',
      releaseVersion,
    })
    versionStatuses.value = {
      ...versionStatuses.value,
      [releaseVersion]: preview.callableReleaseVersions.includes(releaseVersion)
        ? 'PUBLISHED'
        : 'STOPPED',
    }
  } catch {
    // Keep local optimistic state when preview fails.
  }
}

async function loadTemplate() {
  loadFailed.value = false
  try {
    await templatesStore.fetchTemplate(templateId.value)
    syncVersionStatuses()
    const releaseVersion = template.value?.releaseVersion
    if (template.value?.lifecycleStatus === 'PUBLISHED' && releaseVersion) {
      await syncVersionStatusFromPreview(releaseVersion)
    }
    if (showPolicyPanel.value) {
      await loadPolicyData()
    }
  } catch {
    loadFailed.value = true
  }
}

function syncVersionStatuses() {
  const releaseVersion = template.value?.releaseVersion
  if (template.value?.lifecycleStatus === 'PUBLISHED' && releaseVersion) {
    versionStatuses.value = {
      ...versionStatuses.value,
      [releaseVersion]: versionStatuses.value[releaseVersion] ?? 'PUBLISHED',
    }
  }
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
    const confirmed = await confirmAction({
      titleKey: 'templates.lifecycle.confirmRejectTitle',
      messageKey: 'templates.lifecycle.confirmRejectMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
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
  const confirmed = await confirmAction({
    titleKey: 'templates.lifecycle.confirmPublishTitle',
    messageKey: 'templates.lifecycle.confirmPublishMessage',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }
  try {
    await templatesStore.publishTemplate(templateId.value, {
      releaseVersion: publishVersion.value,
    })
    await loadPolicyData()
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
    await ElMessageBox.confirm(impactMessage, t('templates.governance.impactPreviewTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch {
    return
  }

  const confirmed = await confirmAction({
    titleKey: config.confirmTitleKey,
    messageKey: config.confirmMessageKey,
    type: 'warning',
  })
  if (!confirmed) {
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

async function handleVersionAction(
  releaseVersion: string,
  action: 'deactivate' | 'restore',
) {
  const previewAction: LifecycleGovernanceAction =
    action === 'deactivate' ? 'DEACTIVATE_VERSION' : 'RESTORE_VERSION'
  const reasonKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateReasonPrompt'
      : 'templates.versions.restoreReasonPrompt'
  const titleKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateTitle'
      : 'templates.versions.restoreTitle'
  const confirmTitleKey =
    action === 'deactivate'
      ? 'templates.versions.confirmDeactivateTitle'
      : 'templates.versions.confirmRestoreTitle'
  const confirmMessageKey =
    action === 'deactivate'
      ? 'templates.versions.confirmDeactivateMessage'
      : 'templates.versions.confirmRestoreMessage'
  const successKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateSuccess'
      : 'templates.versions.restoreSuccess'

  let reason = ''
  try {
    const result = await ElMessageBox.prompt(t(reasonKey), t(titleKey), {
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
    const impactMessage = await buildImpactPreviewMessage(previewAction, releaseVersion)
    await ElMessageBox.confirm(impactMessage, t('templates.governance.impactPreviewTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch {
    return
  }

  const confirmed = await confirmAction({
    titleKey: confirmTitleKey,
    messageKey: confirmMessageKey,
    type: 'warning',
  })
  if (!confirmed) {
    return
  }

  const payload = { reason, confirmed: true }
  try {
    if (action === 'deactivate') {
      await templatesStore.deactivateTemplateVersion(templateId.value, releaseVersion, payload)
      versionStatuses.value = { ...versionStatuses.value, [releaseVersion]: 'STOPPED' }
    } else {
      await templatesStore.restoreTemplateVersion(templateId.value, releaseVersion, payload)
      versionStatuses.value = { ...versionStatuses.value, [releaseVersion]: 'PUBLISHED' }
    }
    await syncVersionStatusFromPreview(releaseVersion)
    ElMessage.success(t(successKey))
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
            <dd>{{ new Date(template.updatedAt).toLocaleString() }}</dd>
          </div>
        </dl>
        <p class="description">
          {{ template.description ?? t('templates.detail.noDescription') }}
        </p>
      </el-card>

      <el-card v-if="showLifecycleSection" shadow="never" class="section-card">
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
            <el-input
              v-model="publishVersion"
              :placeholder="t('templates.lifecycle.releaseVersionPlaceholder')"
              class="publish-input"
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

      <el-card v-if="showVersionsSection" shadow="never" class="section-card">
        <h2>{{ t('templates.versions.title') }}</h2>
        <p class="governance-description">{{ t('templates.versions.description') }}</p>
        <el-table :data="publishedVersions" stripe empty-text="">
          <template #empty>
            <el-empty :description="t('templates.versions.empty')" />
          </template>
          <el-table-column
            prop="releaseVersion"
            :label="t('templates.versions.releaseVersion')"
            min-width="160"
          />
          <el-table-column :label="t('templates.versions.status')" width="160">
            <template #default="{ row }">
              <TemplateStatusBadge :status="row.lifecycleStatus" />
            </template>
          </el-table-column>
          <el-table-column :label="t('templates.versions.actions')" min-width="220">
            <template #default="{ row }">
              <el-button
                v-if="row.lifecycleStatus === 'PUBLISHED'"
                link
                type="warning"
                :loading="templatesStore.submitting"
                @click="handleVersionAction(row.releaseVersion, 'deactivate')"
              >
                {{ t('templates.versions.deactivate') }}
              </el-button>
              <el-button
                v-if="row.lifecycleStatus === 'STOPPED'"
                link
                type="primary"
                :loading="templatesStore.submitting"
                @click="handleVersionAction(row.releaseVersion, 'restore')"
              >
                {{ t('templates.versions.restore') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="showAuthoringSection" shadow="never" class="section-card">
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

      <el-card v-if="showPolicyPanel" shadow="never" class="section-card">
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
              <el-select
                v-model="policyForm.allowedAdGroups"
                multiple
                filterable
                allow-create
                default-first-option
                :placeholder="t('templates.policy.allowedAdGroupsPlaceholder')"
              />
            </el-form-item>
            <el-form-item :label="t('templates.policy.outputFormats')">
              <el-select v-model="policyForm.outputFormats" multiple filterable allow-create>
                <el-option
                  v-for="format in policyOutputFormatOptions"
                  :key="format"
                  :label="format"
                  :value="format"
                />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('templates.policy.outputModes')">
              <el-select v-model="policyForm.outputModes" multiple filterable allow-create>
                <el-option
                  v-for="mode in policyOutputModeOptions"
                  :key="mode"
                  :label="mode"
                  :value="mode"
                />
              </el-select>
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
          <el-table :data="templatesStore.credentials" stripe empty-text="">
            <template #empty>
              <el-empty :description="t('templates.policy.noCredentials')" />
            </template>
            <el-table-column prop="externalId" :label="t('templates.policy.credentialExternalId')" />
            <el-table-column prop="status" :label="t('templates.policy.credentialStatus')" />
            <el-table-column :label="t('templates.policy.credentialCreatedAt')" min-width="180">
              <template #default="{ row }">
                {{ new Date(row.createdAt).toLocaleString() }}
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
          </el-table>
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
    </template>

    <TemplateMetadataEditDialog
      v-if="template"
      v-model="metadataEditOpen"
      :initial-name="template.name"
      :initial-description="template.description"
      :loading="templatesStore.submitting"
      @submit="handleMetadataUpdate"
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

.publish-input {
  width: 160px;
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
