<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import TemplateAuthoringPanel from '@/components/templates/TemplateAuthoringPanel.vue'
import TemplateRuleConfigurator from '@/components/templates/TemplateRuleConfigurator.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import TemplateTestDataSetPanel from '@/components/templates/TemplateTestDataSetPanel.vue'
import { canManageApiPolicy, canManageTemplateLifecycle } from '@/auth/roles'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { PreviewRecord, UpsertApiPolicyPayload } from '@/types/template'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const templatesStore = useTemplatesStore()
const sessionStore = useSessionStore()

const lifecycleComment = ref('')
const publishVersion = ref('1.0.0')
const showCredentialSecret = ref(false)
const lastPreview = ref<PreviewRecord | null>(null)
const selectedTestDataSetId = ref<string | null>(null)

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
const canLifecycle = computed(() =>
  canManageTemplateLifecycle(sessionStore.session?.roles ?? []),
)
const canPolicy = computed(() => canManageApiPolicy(sessionStore.session?.roles ?? []))

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

const showDraftActions = computed(() => template.value?.lifecycleStatus === 'DRAFT')
const showTestingActions = computed(() => template.value?.lifecycleStatus === 'TESTING')
const showApprovalActions = computed(() => template.value?.lifecycleStatus === 'APPROVAL')
const showPublishActions = computed(() => template.value?.lifecycleStatus === 'PENDING_RELEASE')
const showPolicyPanel = computed(
  () => template.value?.lifecycleStatus === 'PUBLISHED' && canPolicy.value,
)

onMounted(async () => {
  await loadTemplate()
})

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
    showCredentialSecret.value = true
    await templatesStore.createCredential(templateId.value)
    ElMessage.success(t('templates.policy.createCredentialSuccess'))
  } catch {
    showCredentialSecret.value = false
    ElMessage.error(errorMessage.value || t('templates.error.createCredential'))
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
            <dd>{{ template.masterId }}</dd>
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

      <el-card v-if="canLifecycle" shadow="never" class="section-card">
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
          <template v-if="showTestingActions">
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
            v-if="showApprovalActions"
            type="primary"
            :loading="templatesStore.submitting"
            @click="handleSubmitForApproval"
          >
            {{ t('templates.lifecycle.submitApproval') }}
          </el-button>
          <template v-if="showApprovalActions">
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
              <ul class="publish-gate-list">
                <li>{{ t('templates.publishGate.lifecycleReady') }}</li>
                <li>{{ t('templates.publishGate.releaseVersionProvided') }} — {{ publishVersion || t('templates.publishGate.pending') }}</li>
                <li>{{ t('templates.publishGate.apiPolicyConfigured') }}</li>
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
              @click="handlePublish"
            >
              {{ t('templates.lifecycle.publish') }}
            </el-button>
          </template>
          <el-button
            :loading="templatesStore.submitting"
            @click="handleTestGenerate"
          >
            {{ t('templates.testGenerate.action') }}
          </el-button>
        </div>
      </el-card>

      <el-card v-if="canLifecycle" shadow="never" class="section-card">
        <h2>{{ t('templates.authoring.title') }}</h2>
        <TemplateAuthoringPanel
          :template-id="templateId"
          :variables="template.variables"
          :bindings="template.bindings"
        />
        <TemplateRuleConfigurator :template-id="templateId" />
        <h3>{{ t('templates.testDataSets.title') }}</h3>
        <TemplateTestDataSetPanel
          :template-id="templateId"
          @selected="(id) => { selectedTestDataSetId = id }"
        />
      </el-card>

      <el-card v-if="canLifecycle" shadow="never" class="section-card">
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
          <el-alert
            v-if="showCredentialSecret && templatesStore.lastCreatedCredential"
            type="warning"
            :title="t('templates.policy.credentialCreatedTitle')"
            :closable="false"
            show-icon
            class="credential-alert"
          >
            <p>{{ t('templates.policy.credentialExternalId') }}: {{ templatesStore.lastCreatedCredential.externalId }}</p>
            <p>{{ t('templates.policy.credentialSecretHint') }}</p>
          </el-alert>
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
          </el-table>
        </template>
      </el-card>

      <el-card v-if="showPolicyPanel" shadow="never" class="section-card">
        <h2>{{ t('templates.contract.title') }}</h2>
        <TemplateCallerContractPanel :template-id="templateId" environment="dev" />
      </el-card>
    </template>
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

.credential-alert {
  margin: 1rem 0;

  p {
    margin: 0.25rem 0;
  }
}
</style>
