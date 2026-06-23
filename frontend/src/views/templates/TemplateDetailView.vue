<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import TemplateAuthoringPanel from '@/components/templates/TemplateAuthoringPanel.vue'
import TemplateRuleConfigurator from '@/components/templates/TemplateRuleConfigurator.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import TemplateTestDataSetPanel from '@/components/templates/TemplateTestDataSetPanel.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { MASTER_DETAIL_PATH_PREFIX, ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { BindingValidationResult, PreviewRecord, UpsertApiPolicyPayload } from '@/types/template'
import { ElMessage, ElMessageBox } from 'element-plus'

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
  manageApiPolicy,
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

async function loadTemplate() {
  try {
    await templatesStore.fetchTemplate(templateId.value)
    if (showPolicyPanel.value) {
      await loadPolicyData()
    }
  } catch {
    // Error surfaced via store message key.
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

async function handleGovernanceAction(action: GovernanceAction) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt(
      t(`templates.lifecycle.${action}ReasonPrompt`),
      t(`templates.lifecycle.${action}Title`),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValidator: (value) =>
          value.trim().length > 0 ? true : t('templates.lifecycle.reasonRequired'),
      },
    )
    reason = result.value.trim()
  } catch {
    return
  }

  const confirmKeys = {
    stop: {
      titleKey: 'templates.lifecycle.confirmStopTitle',
      messageKey: 'templates.lifecycle.confirmStopMessage',
    },
    restore: {
      titleKey: 'templates.lifecycle.confirmRestoreTitle',
      messageKey: 'templates.lifecycle.confirmRestoreMessage',
    },
    deprecate: {
      titleKey: 'templates.lifecycle.confirmDeprecateTitle',
      messageKey: 'templates.lifecycle.confirmDeprecateMessage',
    },
  } as const

  const confirmed = await confirmAction({
    ...confirmKeys[action],
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
    ElMessage.success(t(`templates.lifecycle.${action}Success`))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
  }
}

async function handleSavePolicy() {
  try {
    await templatesStore.saveApiPolicy(templateId.value, { ...policyForm })
    ElMessage.success(t('templates.policy.saveSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.savePolicy'))
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
        <TemplateStatusBadge :status="template.lifecycleStatus" />
      </div>
    </header>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="templatesStore.loadingDetail" :rows="8" animated />

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
            <el-form-item :label="t('templates.policy.batchEnabled')">
              <el-switch v-model="policyForm.batchEnabled" />
            </el-form-item>
            <el-form-item :label="t('templates.policy.maxBatchSize')">
              <el-input-number v-model="policyForm.maxBatchSize" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item :label="t('templates.policy.docxEncryptionEnabled')">
              <el-switch v-model="policyForm.docxEncryptionEnabled" />
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
        <TemplateCallerContractPanel :template-id="templateId" environment="dev" />
      </el-card>
    </template>

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
